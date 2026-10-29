package kz.nurbolat.bookaggregator.auth;

import kz.nurbolat.bookaggregator.admin.SystemEventService;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.security.JwtProperties;
import kz.nurbolat.bookaggregator.security.TokenHasher;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh tokens with rotation. A sign-in starts a family; every refresh revokes the token it used and issues
 * the next one of the same family. A revoked token coming back means it was copied, so its family is revoked:
 * the thief and the owner of that session sign in again, the user's other devices stay signed in.
 * Two tabs refreshing with the same token at once must not look like theft: the token that replaces a token
 * is not random but derived from it (HMAC-SHA256 of the previous token with the JWT secret), so a revoked token
 * presented within {@code app.jwt.refresh-reuse-grace} of its revocation can be answered with the family's
 * current token without storing any raw token. A revoked token whose derived chain does not reach the family's
 * active token (the session was ended by logout, password change or the session limit), or one presented after the
 * window, is treated as a copy. Parallel requests with one token serialize on the row lock of that token.
 * A user keeps at most {@code app.jwt.max-sessions} families; a new sign-in over the limit ends the one
 * refreshed least recently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int USER_AGENT_MAX = 255;
    private static final String EVENT_SOURCE = "auth";
    private static final String SUCCESSOR_PREFIX = "kr-refresh-successor:";
    private static final int SUCCESSOR_STEPS = 3;

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final JwtProperties properties;
    private final SystemEventService systemEventService;
    private final Clock clock;

    /**
     * Starts a new session (family) of the user.
     *
     * @return the new token itself (only its hash is stored) and the family of the session
     */
    @Transactional
    public IssuedToken issue(User user, String userAgent) {
        endSessionsOverLimit(user);
        return save(user, UUID.randomUUID(), TokenHasher.newToken(), userAgent);
    }

    /**
     * Revokes the token and issues the next one of its family.
     *
     * @throws AppException {@code TOKEN_EXPIRED} for an expired token, {@code UNAUTHORIZED} for an unknown or revoked one
     */
    // The revocation after a reuse must stay although the call fails
    @Transactional(noRollbackFor = AppException.class)
    public IssuedToken rotate(String token, String userAgent) {
        RefreshToken current = repository.findByTokenHash(TokenHasher.hash(token))
                .orElseThrow(RefreshTokenService::invalid);
        Instant now = clock.instant();
        User user = current.getUser();
        if (current.getRevokedAt() != null) {
            if (current.getRevokedAt().plus(properties.refreshReuseGrace()).isAfter(now)) {
                Optional<String> successor = activeSuccessor(token, current.getFamilyId(), now);
                if (successor.isPresent()) {
                    Map<String, Object> details = new HashMap<>();
                    details.put("userId", user.getId());
                    details.put("familyId", current.getFamilyId().toString());
                    details.put("userAgent", truncate(userAgent));
                    systemEventService.info(EVENT_SOURCE, "Повтор refresh-токена в окне: выдан текущий токен", details);
                    return new IssuedToken(user, successor.get(), current.getFamilyId());
                }
            }
            int revoked = repository.revokeFamily(current.getFamilyId(), now);
            log.warn("Revoked refresh token used again: user={}, family={}, revoked {} active tokens",
                    user.getId(), current.getFamilyId(), revoked);
            Map<String, Object> details = new HashMap<>();
            details.put("userId", user.getId());
            details.put("familyId", current.getFamilyId().toString());
            details.put("userAgent", truncate(userAgent));
            systemEventService.warn(EVENT_SOURCE, "Повторное использование отозванного refresh-токена: сессия завершена",
                    details);
            throw invalid();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "Сессия истекла, войдите снова");
        }
        current.setRevokedAt(now);
        return save(user, current.getFamilyId(), successorOf(token), userAgent);
    }

    /**
     * Signs the session of the token out, whatever the state of the token: a tab logging out with a token that the
     * other tab has just rotated must end the session too. An unknown token is ignored.
     */
    @Transactional
    public void revoke(String token) {
        repository.findByTokenHash(TokenHasher.hash(token))
                .ifPresent(t -> repository.revokeFamily(t.getFamilyId(), clock.instant()));
    }

    /** Signs the user out everywhere (password changed). */
    @Transactional
    public void revokeAll(Long userId) {
        repository.revokeAllOfUser(userId, clock.instant());
    }

    /** Family of the token if it is an active token of this user; empty otherwise. */
    @Transactional
    public Optional<UUID> activeFamilyOf(String token, Long userId) {
        Instant now = clock.instant();
        return repository.findByTokenHash(TokenHasher.hash(token))
                .filter(t -> t.getRevokedAt() == null && t.getExpiresAt().isAfter(now))
                .filter(t -> t.getUser().getId().equals(userId))
                .map(RefreshToken::getFamilyId);
    }

    /** Signs the user out everywhere except the session of the given family (password changed on this device). */
    @Transactional
    public void revokeAllExcept(Long userId, UUID familyId) {
        repository.revokeAllExceptFamily(userId, familyId, clock.instant());
    }

    // A family has at most one active token, so active tokens are the user's sessions. The user's row is locked
    // first: two sign-ins at once would otherwise both see room for one more session and go over the limit
    private void endSessionsOverLimit(User user) {
        userRepository.lockById(user.getId());
        Instant now = clock.instant();
        List<RefreshToken> active = repository.findActiveOfUser(user.getId(), now);
        int over = active.size() - properties.maxSessions() + 1;
        for (int i = 0; i < over; i++) {
            active.get(i).setRevokedAt(now);
        }
        if (over > 0) {
            log.info("Session limit of user {} reached: {} least recently used sessions ended", user.getId(), over);
        }
    }

    // Follows the derived chain from the presented token to the family's active token (at most a few rotations)
    private Optional<String> activeSuccessor(String token, UUID familyId, Instant now) {
        Optional<RefreshToken> active = repository.findActiveOfFamily(familyId, now);
        if (active.isEmpty()) {
            return Optional.empty();
        }
        String candidate = token;
        for (int i = 0; i < SUCCESSOR_STEPS; i++) {
            candidate = successorOf(candidate);
            if (TokenHasher.matches(candidate, active.get().getTokenHash())) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private String successorOf(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] derived = mac.doFinal((SUCCESSOR_PREFIX + token).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    private IssuedToken save(User user, UUID familyId, String token, String userAgent) {
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setFamilyId(familyId);
        entity.setTokenHash(TokenHasher.hash(token));
        entity.setExpiresAt(clock.instant().plus(properties.refreshTtl()));
        entity.setUserAgent(truncate(userAgent));
        repository.save(entity);
        return new IssuedToken(user, token, familyId);
    }

    private static String truncate(String userAgent) {
        return userAgent == null || userAgent.length() <= USER_AGENT_MAX ? userAgent : userAgent.substring(0, USER_AGENT_MAX);
    }

    private static AppException invalid() {
        return new AppException(ErrorCode.UNAUTHORIZED, "Сессия недействительна, войдите снова");
    }

    /**
     * @param token    the refresh token itself
     * @param familyId the session the token belongs to
     */
    public record IssuedToken(User user, String token, UUID familyId) {
    }
}

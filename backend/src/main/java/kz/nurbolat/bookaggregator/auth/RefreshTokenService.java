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

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Refresh tokens with rotation. A sign-in starts a family; every refresh revokes the token it used and issues
 * the next one of the same family. A revoked token coming back means it was copied, so its family is revoked:
 * the thief and the owner of that session sign in again, the user's other devices stay signed in.
 * A user keeps at most {@code app.jwt.max-sessions} families; a new sign-in over the limit ends the one
 * refreshed least recently.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int USER_AGENT_MAX = 255;
    private static final String EVENT_SOURCE = "auth";

    private final RefreshTokenRepository repository;
    private final UserRepository userRepository;
    private final JwtProperties properties;
    private final SystemEventService systemEventService;
    private final Clock clock;

    /**
     * Starts a new session (family) of the user.
     *
     * @return the new token itself; only its hash is stored
     */
    @Transactional
    public String issue(User user, String userAgent) {
        endSessionsOverLimit(user);
        return save(user, UUID.randomUUID(), userAgent);
    }

    /**
     * Revokes the token and issues the next one of its family.
     *
     * @throws AppException {@code TOKEN_EXPIRED} for an expired token, {@code UNAUTHORIZED} for an unknown or revoked one
     */
    // The revocation after a reuse must stay although the call fails
    @Transactional(noRollbackFor = AppException.class)
    public Rotated rotate(String token, String userAgent) {
        RefreshToken current = repository.findByTokenHash(TokenHasher.hash(token))
                .orElseThrow(RefreshTokenService::invalid);
        Instant now = clock.instant();
        User user = current.getUser();
        if (current.getRevokedAt() != null) {
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
        return new Rotated(user, save(user, current.getFamilyId(), userAgent));
    }

    /** Signs one session out; an unknown or already revoked token is ignored. */
    @Transactional
    public void revoke(String token) {
        repository.findByTokenHash(TokenHasher.hash(token))
                .filter(t -> t.getRevokedAt() == null)
                .ifPresent(t -> t.setRevokedAt(clock.instant()));
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

    private String save(User user, UUID familyId, String userAgent) {
        String token = TokenHasher.newToken();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setFamilyId(familyId);
        entity.setTokenHash(TokenHasher.hash(token));
        entity.setExpiresAt(clock.instant().plus(properties.refreshTtl()));
        entity.setUserAgent(truncate(userAgent));
        repository.save(entity);
        return token;
    }

    private static String truncate(String userAgent) {
        return userAgent == null || userAgent.length() <= USER_AGENT_MAX ? userAgent : userAgent.substring(0, USER_AGENT_MAX);
    }

    private static AppException invalid() {
        return new AppException(ErrorCode.UNAUTHORIZED, "Сессия недействительна, войдите снова");
    }

    /**
     * @param token the next refresh token itself
     */
    public record Rotated(User user, String token) {
    }
}

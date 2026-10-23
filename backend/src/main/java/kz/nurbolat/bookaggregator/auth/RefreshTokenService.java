package kz.nurbolat.bookaggregator.auth;

import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.security.JwtProperties;
import kz.nurbolat.bookaggregator.security.TokenHasher;
import kz.nurbolat.bookaggregator.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Refresh tokens with rotation: every refresh revokes the token it used and issues the next one. A revoked
 * token coming back means it was copied, so all tokens of that user are revoked and everyone signs in again.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int USER_AGENT_MAX = 255;

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final Clock clock;

    /**
     * @return the new token itself; only its hash is stored
     */
    @Transactional
    public String issue(User user, String userAgent) {
        String token = TokenHasher.newToken();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(TokenHasher.hash(token));
        entity.setExpiresAt(clock.instant().plus(properties.refreshTtl()));
        entity.setUserAgent(userAgent == null || userAgent.length() <= USER_AGENT_MAX
                ? userAgent : userAgent.substring(0, USER_AGENT_MAX));
        repository.save(entity);
        return token;
    }

    /**
     * Revokes the token and issues the next one for the same user.
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
            int revoked = repository.revokeAllOfUser(user.getId(), now);
            log.warn("Revoked refresh token used again: user={}, revoked {} active tokens", user.getId(), revoked);
            throw invalid();
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "Сессия истекла, войдите снова");
        }
        current.setRevokedAt(now);
        return new Rotated(user, issue(user, userAgent));
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

    private static AppException invalid() {
        return new AppException(ErrorCode.UNAUTHORIZED, "Сессия недействительна, войдите снова");
    }

    /**
     * @param token the next refresh token itself
     */
    public record Rotated(User user, String token) {
    }
}

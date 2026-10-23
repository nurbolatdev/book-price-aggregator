package kz.nurbolat.bookaggregator.auth;

import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.security.TokenHasher;
import kz.nurbolat.bookaggregator.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * One-time tokens for links in emails. The token goes into the email, the database keeps its hash.
 */
@Service
@RequiredArgsConstructor
public class EmailTokenService {

    private final EmailTokenRepository repository;
    private final Clock clock;

    /**
     * @return the token itself, to be put into the link
     */
    @Transactional
    public String issue(User user, EmailTokenPurpose purpose, Duration ttl) {
        String token = TokenHasher.newToken();
        EmailToken entity = new EmailToken();
        entity.setUser(user);
        entity.setTokenHash(TokenHasher.hash(token));
        entity.setPurpose(purpose);
        entity.setExpiresAt(clock.instant().plus(ttl));
        repository.save(entity);
        return token;
    }

    /**
     * Uses the token up and returns whose it is.
     *
     * @throws AppException {@code TOKEN_INVALID} for an unknown, used or other-purpose token,
     *                      {@code TOKEN_EXPIRED} for an expired one
     */
    @Transactional
    public User consume(String token, EmailTokenPurpose purpose) {
        EmailToken entity = repository.findByTokenHash(TokenHasher.hash(token))
                .filter(t -> t.getPurpose() == purpose)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID, "Ссылка недействительна"));
        if (entity.getUsedAt() != null) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Ссылка уже использована");
        }
        Instant now = clock.instant();
        if (!entity.getExpiresAt().isAfter(now)) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "Срок действия ссылки истёк");
        }
        entity.setUsedAt(now);
        return entity.getUser();
    }

    /** Whether a token of this purpose was issued to the user after the given moment. */
    @Transactional(readOnly = true)
    public boolean issuedSince(User user, EmailTokenPurpose purpose, Instant since) {
        return repository.existsByUserIdAndPurposeAndCreatedAtAfter(user.getId(), purpose, since);
    }
}

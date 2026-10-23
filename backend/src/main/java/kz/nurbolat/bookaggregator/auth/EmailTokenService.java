package kz.nurbolat.bookaggregator.auth;

import kz.nurbolat.bookaggregator.beacon.Beacon;
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
        return issue(user, purpose, ttl, null);
    }

    /**
     * Unsubscribe link of a beacon: works any number of times until it expires.
     */
    @Transactional
    public String issueUnsubscribe(Beacon beacon, Duration ttl) {
        return issue(beacon.getUser(), EmailTokenPurpose.UNSUBSCRIBE, ttl, beacon);
    }

    private String issue(User user, EmailTokenPurpose purpose, Duration ttl, Beacon beacon) {
        String token = TokenHasher.newToken();
        EmailToken entity = new EmailToken();
        entity.setUser(user);
        entity.setBeacon(beacon);
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
        EmailToken entity = valid(token, purpose);
        if (entity.getUsedAt() != null) {
            throw new AppException(ErrorCode.TOKEN_INVALID, "Ссылка уже использована");
        }
        entity.setUsedAt(clock.instant());
        return entity.getUser();
    }

    /**
     * The beacon of an unsubscribe link; the link stays valid for further clicks.
     *
     * @throws AppException {@code TOKEN_INVALID} for an unknown or other-purpose token, {@code TOKEN_EXPIRED} for an expired one
     */
    @Transactional
    public Beacon unsubscribeBeacon(String token) {
        EmailToken entity = valid(token, EmailTokenPurpose.UNSUBSCRIBE);
        entity.setUsedAt(clock.instant());
        return entity.getBeacon();
    }

    private EmailToken valid(String token, EmailTokenPurpose purpose) {
        EmailToken entity = repository.findByTokenHash(TokenHasher.hash(token))
                .filter(t -> t.getPurpose() == purpose)
                .orElseThrow(() -> new AppException(ErrorCode.TOKEN_INVALID, "Ссылка недействительна"));
        if (!entity.getExpiresAt().isAfter(clock.instant())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "Срок действия ссылки истёк");
        }
        return entity;
    }

    /** Whether a token of this purpose was issued to the user after the given moment. */
    @Transactional(readOnly = true)
    public boolean issuedSince(User user, EmailTokenPurpose purpose, Instant since) {
        return repository.existsByUserIdAndPurposeAndCreatedAtAfter(user.getId(), purpose, since);
    }
}

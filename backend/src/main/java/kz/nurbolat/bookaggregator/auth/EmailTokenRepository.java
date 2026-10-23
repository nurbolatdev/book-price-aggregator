package kz.nurbolat.bookaggregator.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    /** Locks the row, so that a token cannot be used twice by two requests at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndPurposeAndCreatedAtAfter(Long userId, EmailTokenPurpose purpose, Instant after);

    /** Deletes up to {@code limit} tokens that expired before the given moment; returns how many. */
    @Modifying
    @Query(value = """
            DELETE FROM email_tokens WHERE id IN (
                SELECT id FROM email_tokens WHERE expires_at < :before LIMIT :limit)""", nativeQuery = true)
    int deleteExpiredBefore(Instant before, int limit);
}

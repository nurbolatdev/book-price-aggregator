package kz.nurbolat.bookaggregator.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    /** Locks the row, so that a token cannot be used twice by two requests at once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndPurposeAndCreatedAtAfter(Long userId, EmailTokenPurpose purpose, Instant after);
}

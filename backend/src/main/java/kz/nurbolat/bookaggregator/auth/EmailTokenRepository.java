package kz.nurbolat.bookaggregator.auth;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<EmailToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndPurposeAndCreatedAtAfter(Long userId, EmailTokenPurpose purpose, Instant after);
}

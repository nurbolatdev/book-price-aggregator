package kz.nurbolat.bookaggregator.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /** Locks the row, so that two refreshes with the same token cannot both succeed. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.user.id = :userId AND t.revokedAt IS NULL")
    int revokeAllOfUser(Long userId, Instant now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.user.id = :userId AND t.familyId <> :familyId AND t.revokedAt IS NULL")
    int revokeAllExceptFamily(Long userId, UUID familyId, Instant now);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revokedAt = :now WHERE t.familyId = :familyId AND t.revokedAt IS NULL")
    int revokeFamily(UUID familyId, Instant now);

    /** Sessions of the user that can still be refreshed, least recently refreshed first. */
    @Query("""
            SELECT t FROM RefreshToken t
            WHERE t.user.id = :userId AND t.revokedAt IS NULL AND t.expiresAt > :now
            ORDER BY t.createdAt, t.id""")
    List<RefreshToken> findActiveOfUser(Long userId, Instant now);

    /** Deletes up to {@code limit} tokens that expired before the given moment; returns how many. */
    @Modifying
    @Query(value = """
            DELETE FROM refresh_tokens WHERE id IN (
                SELECT id FROM refresh_tokens WHERE expires_at < :before LIMIT :limit)""", nativeQuery = true)
    int deleteExpiredBefore(Instant before, int limit);
}

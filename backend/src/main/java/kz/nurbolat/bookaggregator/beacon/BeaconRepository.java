package kz.nurbolat.bookaggregator.beacon;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BeaconRepository extends JpaRepository<Beacon, Long> {

    List<Beacon> findByUserId(Long userId);

    List<Beacon> findByEditionIdAndStatus(Long editionId, BeaconStatus status);

    Optional<Beacon> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndEditionId(Long userId, Long editionId);

    /**
     * The next batch of beacons of an edition in the given statuses after {@code afterId}, user loaded, rows
     * locked: two price changes of one edition checked at once cannot both report the same drop.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM Beacon b JOIN FETCH b.user
            WHERE b.edition.id = :editionId AND b.status IN :statuses AND b.id > :afterId
            ORDER BY b.id""")
    List<Beacon> findBatchForUpdate(Long editionId, Collection<BeaconStatus> statuses, Long afterId, Pageable page);
}

package kz.nurbolat.bookaggregator.beacon;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BeaconRepository extends JpaRepository<Beacon, Long> {

    List<Beacon> findByUserId(Long userId);

    List<Beacon> findByEditionIdAndStatus(Long editionId, BeaconStatus status);

    Optional<Beacon> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndEditionId(Long userId, Long editionId);
}

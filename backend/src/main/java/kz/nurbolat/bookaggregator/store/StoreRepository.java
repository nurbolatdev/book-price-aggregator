package kz.nurbolat.bookaggregator.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, Long> {

    Optional<Store> findByCode(StoreCode code);

    List<Store> findAllByOrderByIdAsc();
}

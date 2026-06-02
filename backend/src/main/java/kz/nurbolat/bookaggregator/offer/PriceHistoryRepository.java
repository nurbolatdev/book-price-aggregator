package kz.nurbolat.bookaggregator.offer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByOfferIdOrderByRecordedAtAsc(Long offerId);
}

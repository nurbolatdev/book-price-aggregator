package kz.nurbolat.bookaggregator.offer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByBookIdOrderByPriceAsc(Long bookId);

    List<Offer> findByBookIdAndSource(Long bookId, String source);

    void deleteByBookIdAndSource(Long bookId, String source);
}

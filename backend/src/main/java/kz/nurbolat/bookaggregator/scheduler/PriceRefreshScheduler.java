package kz.nurbolat.bookaggregator.scheduler;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.favorite.FavoriteRepository;
import kz.nurbolat.bookaggregator.offer.Offer;
import kz.nurbolat.bookaggregator.offer.OfferRepository;
import kz.nurbolat.bookaggregator.offer.PriceHistory;
import kz.nurbolat.bookaggregator.offer.PriceHistoryRepository;
import kz.nurbolat.bookaggregator.pricealert.PriceAlert;
import kz.nurbolat.bookaggregator.pricealert.PriceAlertRepository;
import kz.nurbolat.bookaggregator.source.BookSource;
import kz.nurbolat.bookaggregator.source.BookSourceResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceRefreshScheduler {

    private final FavoriteRepository favoriteRepository;
    private final OfferRepository offerRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final List<BookSource> sources;

    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void refreshPrices() {
        log.info("Starting price refresh for favorited books");

        List<Book> favoritedBooks = favoriteRepository.findAll().stream()
                .map(fav -> fav.getBook())
                .distinct()
                .toList();

        for (Book book : favoritedBooks) {
            try {
                refreshBookPrices(book);
            } catch (Exception e) {
                log.error("Failed to refresh prices for book {}: {}", book.getId(), e.getMessage());
            }
        }

        checkPriceAlerts();
        log.info("Price refresh completed for {} books", favoritedBooks.size());
    }

    private void refreshBookPrices(Book book) {
        for (BookSource source : sources) {
            List<BookSourceResult> results = source.search(book.getTitle());
            if (results.isEmpty()) continue;

            BookSourceResult result = results.get(0);
            List<Offer> existing = offerRepository.findByBookIdAndSource(book.getId(), source.getName());

            Offer offer = existing.isEmpty() ? new Offer() : existing.get(0);
            offer.setBook(book);
            offer.setSource(source.getName());
            offer.setPrice(result.getPrice());
            offer.setOriginalPrice(result.getOriginalPrice());
            offer.setInStock(result.isInStock());
            offer.setUrl(result.getUrl());
            offer.setFetchedAt(LocalDateTime.now());
            offerRepository.save(offer);

            PriceHistory history = new PriceHistory();
            history.setOffer(offer);
            history.setPrice(result.getPrice());
            priceHistoryRepository.save(history);
        }
    }

    private void checkPriceAlerts() {
        List<PriceAlert> activeAlerts = priceAlertRepository.findByActiveTrue();

        for (PriceAlert alert : activeAlerts) {
            List<Offer> offers = offerRepository.findByBookIdOrderByPriceAsc(alert.getBook().getId());
            if (offers.isEmpty()) continue;

            Offer bestOffer = offers.get(0);
            if (bestOffer.getPrice().compareTo(alert.getTargetPrice()) <= 0) {
                log.info("Price alert triggered for book {} — current price: {}, target: {}",
                        alert.getBook().getTitle(), bestOffer.getPrice(), alert.getTargetPrice());
                alert.setTriggeredAt(LocalDateTime.now());
                alert.setActive(false);
                priceAlertRepository.save(alert);
            }
        }
    }
}

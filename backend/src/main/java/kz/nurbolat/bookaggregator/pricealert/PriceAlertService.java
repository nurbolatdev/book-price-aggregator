package kz.nurbolat.bookaggregator.pricealert;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.book.BookRepository;
import kz.nurbolat.bookaggregator.common.ResourceNotFoundException;
import kz.nurbolat.bookaggregator.security.SecurityUtils;
import kz.nurbolat.bookaggregator.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final BookRepository bookRepository;
    private final SecurityUtils securityUtils;

    public List<PriceAlert> getAlerts() {
        User user = securityUtils.getCurrentUser();
        return priceAlertRepository.findByUserId(user.getId());
    }

    @Transactional
    public PriceAlert createAlert(Long bookId, BigDecimal targetPrice) {
        User user = securityUtils.getCurrentUser();
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        if (priceAlertRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            PriceAlert existing = priceAlertRepository.findByUserIdAndBookId(user.getId(), bookId).orElseThrow();
            existing.setTargetPrice(targetPrice);
            existing.setActive(true);
            existing.setTriggeredAt(null);
            return priceAlertRepository.save(existing);
        }

        PriceAlert alert = new PriceAlert();
        alert.setUser(user);
        alert.setBook(book);
        alert.setTargetPrice(targetPrice);
        return priceAlertRepository.save(alert);
    }

    @Transactional
    public void deleteAlert(Long bookId) {
        User user = securityUtils.getCurrentUser();
        priceAlertRepository.findByUserIdAndBookId(user.getId(), bookId)
                .ifPresent(priceAlertRepository::delete);
    }
}

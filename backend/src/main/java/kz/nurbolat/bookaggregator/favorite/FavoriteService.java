package kz.nurbolat.bookaggregator.favorite;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.book.BookRepository;
import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.common.ResourceNotFoundException;
import kz.nurbolat.bookaggregator.offer.OfferRepository;
import kz.nurbolat.bookaggregator.security.SecurityUtils;
import kz.nurbolat.bookaggregator.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;
    private final OfferRepository offerRepository;
    private final SecurityUtils securityUtils;

    public List<BookResponse> getFavorites() {
        User user = securityUtils.getCurrentUser();
        return favoriteRepository.findByUserId(user.getId()).stream()
                .map(fav -> BookResponse.from(
                        fav.getBook(),
                        offerRepository.findByBookIdOrderByPriceAsc(fav.getBook().getId())
                ))
                .toList();
    }

    @Transactional
    public void addFavorite(Long bookId) {
        User user = securityUtils.getCurrentUser();
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found: " + bookId));

        if (favoriteRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            return;
        }

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setBook(book);
        favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(Long bookId) {
        User user = securityUtils.getCurrentUser();
        favoriteRepository.deleteByUserIdAndBookId(user.getId(), bookId);
    }

    public boolean isFavorite(Long bookId) {
        User user = securityUtils.getCurrentUser();
        return favoriteRepository.existsByUserIdAndBookId(user.getId(), bookId);
    }
}

package kz.nurbolat.bookaggregator.favorite;

import kz.nurbolat.bookaggregator.book.Book;
import kz.nurbolat.bookaggregator.book.BookRepository;
import kz.nurbolat.bookaggregator.book.dto.BookResponse;
import kz.nurbolat.bookaggregator.common.error.NotFoundException;
import kz.nurbolat.bookaggregator.security.SecurityUtils;
import kz.nurbolat.bookaggregator.support.TestData;
import kz.nurbolat.bookaggregator.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoriteServiceTest {

    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final BookRepository bookRepository = mock(BookRepository.class);
    private final SecurityUtils securityUtils = mock(SecurityUtils.class);
    private final FavoriteService service = new FavoriteService(favoriteRepository, bookRepository, securityUtils);

    private final User user = TestData.user();

    @BeforeEach
    void signIn() {
        user.setId(5L);
        when(securityUtils.getCurrentUser()).thenReturn(user);
    }

    @Test
    void shouldListFavoriteBooks() {
        Book book = TestData.book("Мастер и Маргарита", "Булгаков");
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setBook(book);
        when(favoriteRepository.findByUserId(5L)).thenReturn(List.of(favorite));

        List<BookResponse> result = service.getFavorites();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Мастер и Маргарита");
    }

    @Test
    void shouldSaveNewFavorite() {
        Book book = TestData.book("Dune", "Herbert");
        when(bookRepository.findById(7L)).thenReturn(Optional.of(book));
        when(favoriteRepository.existsByUserIdAndBookId(5L, 7L)).thenReturn(false);

        service.addFavorite(7L);

        ArgumentCaptor<Favorite> saved = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getBook()).isSameAs(book);
    }

    @Test
    void shouldNotDuplicateExistingFavorite() {
        when(bookRepository.findById(7L)).thenReturn(Optional.of(TestData.book("Dune", "Herbert")));
        when(favoriteRepository.existsByUserIdAndBookId(5L, 7L)).thenReturn(true);

        service.addFavorite(7L);

        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnknownBook() {
        when(bookRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addFavorite(404L)).isInstanceOf(NotFoundException.class);
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void shouldRemoveFavoriteOfCurrentUser() {
        service.removeFavorite(7L);

        verify(favoriteRepository).deleteByUserIdAndBookId(5L, 7L);
    }

    @Test
    void shouldTellWhetherBookIsFavorite() {
        when(favoriteRepository.existsByUserIdAndBookId(5L, 7L)).thenReturn(true);
        when(favoriteRepository.existsByUserIdAndBookId(5L, 8L)).thenReturn(false);

        assertThat(service.isFavorite(7L)).isTrue();
        assertThat(service.isFavorite(8L)).isFalse();
    }
}

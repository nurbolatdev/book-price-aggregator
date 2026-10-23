package kz.nurbolat.bookaggregator.book;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.edition.Edition;
import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static kz.nurbolat.bookaggregator.support.TestData.book;
import static kz.nurbolat.bookaggregator.support.TestData.edition;
import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class BookControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;


    @Test
    void shouldReturnBookWithEditionsWhenBookExists() throws Exception {
        Book book = book("Мастер и Маргарита", "Михаил Булгаков");
        Edition hardcover = edition("9780306406157");
        hardcover.setPublisher("АСТ");
        hardcover.setYear((short) 2023);
        hardcover.setBinding(Binding.HARDCOVER);
        hardcover.setCoverUrl("https://img/1.jpg");
        book.addEdition(hardcover);
        book.addEdition(edition(null));
        bookRepository.saveAndFlush(book);

        mockMvc.perform(get("/api/v1/books/{id}", book.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Мастер и Маргарита"))
                .andExpect(jsonPath("$.coverUrl").value("https://img/1.jpg"))
                .andExpect(jsonPath("$.editions", hasSize(2)))
                .andExpect(jsonPath("$.editions[0].isbn13").value("9780306406157"))
                .andExpect(jsonPath("$.editions[0].binding").value("HARDCOVER"))
                .andExpect(jsonPath("$.editions[0].year").value(2023));
    }

    @Test
    void shouldReturn404InErrorFormatWhenBookIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/books/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldReturn404WhenRemovedEndpointsAreCalledByAuthenticatedUser() throws Exception {
        User user = userRepository.save(user());
        String token = "Bearer " + jwtService.generateAccessToken(user);

        mockMvc.perform(get("/api/v1/books/1/offers").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/books/1/price-history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/alerts").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }
}

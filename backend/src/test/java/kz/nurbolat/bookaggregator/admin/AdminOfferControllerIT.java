package kz.nurbolat.bookaggregator.admin;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kz.nurbolat.bookaggregator.offer.MatchStatus;
import kz.nurbolat.bookaggregator.offer.OfferRepository;
import kz.nurbolat.bookaggregator.offer.PriceChangedEvent;
import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.store.StoreCode;
import kz.nurbolat.bookaggregator.support.AbstractIntegrationTest;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static kz.nurbolat.bookaggregator.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@RecordApplicationEvents
class AdminOfferControllerIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;


    @Autowired
    private ApplicationEvents events;

    @PersistenceContext
    private EntityManager entityManager;

    private long editionId;
    private long pendingId;
    private long matchedId;

    @BeforeEach
    void setUp() {
        Long bookId = jdbc.queryForObject("""
                INSERT INTO books (title, author, title_norm, author_norm)
                VALUES ('Пикник на обочине', 'Аркадий Стругацкий', 'пикник на обочине', 'аркадий стругацкий')
                RETURNING id""", Long.class);
        editionId = jdbc.queryForObject("INSERT INTO editions (book_id, publisher) VALUES (?, 'АСТ') RETURNING id",
                Long.class, bookId);
        pendingId = offer("review-1", null, "PENDING_REVIEW", "Пикник на обочине. Роман");
        matchedId = offer("review-2", editionId, "MATCHED", "Пикник на обочине");
    }

    @Test
    void shouldListOnlyOffersWaitingForReview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/offers/pending-review").header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id").value(hasItem((int) pendingId)))
                .andExpect(jsonPath("$.content[*].id").value(not(hasItem((int) matchedId))))
                .andExpect(jsonPath("$.content[?(@.id == %d)].store".formatted(pendingId)).value("FLIP"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].title".formatted(pendingId)).value("Пикник на обочине. Роман"))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void shouldMatchOfferSoThatItCountsForEditionPrices() throws Exception {
        mockMvc.perform(post("/api/v1/admin/offers/{id}/match", pendingId)
                        .header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"editionId\": " + editionId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchStatus").value("MATCHED"))
                .andExpect(jsonPath("$.editionId").value(editionId));

        assertThat(offerRepository.findByEditionWithStore(editionId, MatchStatus.MATCHED))
                .extracting(offer -> offer.getId()).contains(pendingId, matchedId);
        assertThat(events.stream(PriceChangedEvent.class)).singleElement().satisfies(event -> {
            assertThat(event.offerId()).isEqualTo(pendingId);
            assertThat(event.editionId()).isEqualTo(editionId);
            assertThat(event.store()).isEqualTo(StoreCode.FLIP);
            assertThat(event.oldPrice()).isNull();
            assertThat(event.newPrice()).isEqualByComparingTo("2500");
        });
    }

    @Test
    void shouldRejectOfferAndUnlinkIt() throws Exception {
        mockMvc.perform(post("/api/v1/admin/offers/{id}/reject", matchedId)
                        .header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchStatus").value("REJECTED"))
                .andExpect(jsonPath("$.editionId").doesNotExist());
        entityManager.flush();

        assertThat(jdbc.queryForObject("SELECT edition_id FROM offers WHERE id = ?", Long.class, matchedId)).isNull();
        assertThat(events.stream(PriceChangedEvent.class)).isEmpty();
    }

    @Test
    void shouldAnswer404WhenOfferOrEditionIsUnknown() throws Exception {
        String admin = token(User.Role.ADMIN);
        mockMvc.perform(post("/api/v1/admin/offers/{id}/reject", Long.MAX_VALUE).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/admin/offers/{id}/match", pendingId)
                        .header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"editionId\": " + Long.MAX_VALUE + "}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void shouldRequireEditionIdToMatch() throws Exception {
        mockMvc.perform(post("/api/v1/admin/offers/{id}/match", pendingId)
                        .header(HttpHeaders.AUTHORIZATION, token(User.Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("editionId"));
    }

    @Test
    void shouldForbidRegularUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/offers/pending-review").header(HttpHeaders.AUTHORIZATION, token(User.Role.USER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/admin/offers/{id}/reject", pendingId).header(HttpHeaders.AUTHORIZATION, token(User.Role.USER)))
                .andExpect(status().isForbidden());
    }

    private long offer(String externalId, Long edition, String status, String title) {
        return jdbc.queryForObject("""
                INSERT INTO offers (edition_id, store_id, external_id, url, title_raw, price, match_status,
                                    first_seen_at, last_seen_at, last_changed_at)
                SELECT ?, id, ?, 'https://www.flip.kz/catalog?prod=' || ?, ?, 2500, ?, NOW(), NOW(), NOW()
                FROM stores WHERE code = 'FLIP'
                RETURNING id""", Long.class, edition, externalId, externalId, title, status);
    }

    private String token(User.Role role) {
        User user = user();
        user.setRole(role);
        userRepository.save(user);
        return "Bearer " + jwtService.generateAccessToken(user);
    }
}

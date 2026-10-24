package kz.nurbolat.bookaggregator.admin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminParserServiceTest {

    @Test
    void shouldCountSuccessShareOfPages() {
        assertThat(AdminParserService.successPercent(97, 3)).isEqualTo(97.0);
        assertThat(AdminParserService.successPercent(2, 1)).isEqualTo(66.7);
    }

    @Test
    void shouldHaveNoShareWhenNoPagesWereLoaded() {
        assertThat(AdminParserService.successPercent(0, 0)).isNull();
    }
}

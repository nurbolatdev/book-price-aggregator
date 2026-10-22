package kz.nurbolat.bookaggregator.parser.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RobotsRulesTest {

    private static final String TOKEN = "KitapRadarBot";

    @Test
    void shouldAllowEverythingWhenNoGroupAppliesToUs() {
        // Flip.kz (2026-10): rules only for Yandex and Bingbot
        RobotsRules rules = RobotsRules.parse("""
                User-agent: Yandex
                Disallow: /cart
                Disallow: /search*page

                User-agent: Bingbot
                Disallow: /cart
                Sitemap: https://www.flip.kz/sitemap.xml
                """, TOKEN);

        assertThat(rules.allows("/search?search=x&page=2")).isTrue();
        assertThat(rules.allows("/cart")).isTrue();
    }

    @Test
    void shouldApplyWildcardGroupWithStarAndQuery() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /cart
                Disallow: /search*page
                Disallow: /ajax
                """, TOKEN);

        assertThat(rules.allows("/search?search=abc")).isTrue();
        assertThat(rules.allows("/search?search=abc&page=2")).isFalse();
        assertThat(rules.allows("/cart?id=1")).isFalse();
        assertThat(rules.allows("/catalog?prod=1269662")).isTrue();
    }

    @Test
    void shouldPreferOwnGroupOverWildcard() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /

                User-agent: KitapRadarBot
                Disallow: /private
                """, TOKEN);

        assertThat(rules.allows("/catalog?prod=1")).isTrue();
        assertThat(rules.allows("/private/data")).isFalse();
    }

    @Test
    void shouldLetLongestRuleWinAndAllowWinTies() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /favorites
                Allow: /favorites?collection=
                Disallow: /page
                Allow: /page
                """, TOKEN);

        assertThat(rules.allows("/favorites")).isFalse();
        assertThat(rules.allows("/favorites?collection=5")).isTrue();
        assertThat(rules.allows("/page")).isTrue();
    }

    @Test
    void shouldHonourEndAnchorAndEmptyDisallow() {
        RobotsRules rules = RobotsRules.parse("""
                User-agent: *
                Disallow: /*.pdf$
                Disallow:
                """, TOKEN);

        assertThat(rules.allows("/files/book.pdf")).isFalse();
        assertThat(rules.allows("/files/book.pdf?download=1")).isTrue();
        assertThat(rules.allows("/anything")).isTrue();
    }

    @Test
    void shouldIgnoreCommentsAndCase() {
        RobotsRules rules = RobotsRules.parse("""
                # rules for everyone
                user-agent: *   # all bots
                DISALLOW: /secret  # hidden
                """, TOKEN);

        assertThat(rules.allows("/secret/1")).isFalse();
    }
}

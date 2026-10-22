package kz.nurbolat.bookaggregator.parser.matching;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TrigramSimilarityTest {

    @Test
    void shouldBeOneWhenTextsHaveSameWords() {
        assertThat(TrigramSimilarity.similarity("мастер и маргарита", "маргарита и мастер")).isEqualTo(1.0);
    }

    @Test
    void shouldCountSharedTrigramsLikePgTrgmWhenTitleHasExtraWord() {
        // 17 trigrams of the title, 6 more for "роман": 17 / 23, as SELECT similarity(...) returns
        assertThat(TrigramSimilarity.similarity("мастер и маргарита роман", "мастер и маргарита"))
                .isCloseTo(17.0 / 23, within(1e-9));
    }

    @Test
    void shouldBeZeroWhenNothingIsShared() {
        assertThat(TrigramSimilarity.similarity("трудно быть богом", "пикник на обочине")).isZero();
        assertThat(TrigramSimilarity.similarity(null, "пикник")).isZero();
        assertThat(TrigramSimilarity.similarity("", "пикник")).isZero();
    }
}

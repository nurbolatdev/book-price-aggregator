package kz.nurbolat.bookaggregator.common.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {

    @Test
    void shouldLowerCaseAndReplaceYoWhenTextIsCyrillic() {
        assertThat(TextNormalizer.normalize("Ёжик в ТУМАНЕ")).isEqualTo("ежик в тумане");
    }

    @Test
    void shouldDropQuotesWhenTitleIsQuoted() {
        assertThat(TextNormalizer.normalize("«Мастер и Маргарита»")).isEqualTo("мастер и маргарита");
        assertThat(TextNormalizer.normalize("\"Idiot\"")).isEqualTo("idiot");
    }

    @Test
    void shouldReplacePunctuationWithSpaceAndCollapseWhitespace() {
        assertThat(TextNormalizer.normalize("  Толстой,Лев   Николаевич!  Война—и мир... ")).isEqualTo("толстой лев николаевич война и мир");
    }

    @Test
    void shouldKeepDigitsAndKazakhLetters() {
        assertThat(TextNormalizer.normalize("Абай: Қара сөздер 2023")).isEqualTo("абай қара сөздер 2023");
    }

    @Test
    void shouldReturnNullWhenTextIsNull() {
        assertThat(TextNormalizer.normalize(null)).isNull();
    }
}

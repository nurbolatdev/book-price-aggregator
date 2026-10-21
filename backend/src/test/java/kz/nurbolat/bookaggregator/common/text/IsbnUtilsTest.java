package kz.nurbolat.bookaggregator.common.text;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IsbnUtilsTest {

    @Test
    void shouldKeepIsbn13WhenCheckDigitIsValid() {
        assertThat(IsbnUtils.normalize("9780306406157")).isEqualTo("9780306406157");
    }

    @Test
    void shouldStripHyphensAndSpacesWhenIsbnIsFormatted() {
        assertThat(IsbnUtils.normalize("978-0-306-40615-7")).isEqualTo("9780306406157");
        assertThat(IsbnUtils.normalize("978 0 306 40615 7")).isEqualTo("9780306406157");
    }

    @Test
    void shouldConvertIsbn10ToIsbn13() {
        assertThat(IsbnUtils.normalize("0-306-40615-2")).isEqualTo("9780306406157");
    }

    @Test
    void shouldAcceptXCheckDigitWhenConvertingIsbn10() {
        assertThat(IsbnUtils.normalize("0-8044-2957-x")).isEqualTo("9780804429573");
    }

    @Test
    void shouldReturnNullWhenIsbn13CheckDigitIsWrong() {
        assertThat(IsbnUtils.normalize("9780306406158")).isNull();
    }

    @Test
    void shouldReturnNullWhenIsbn10CheckDigitIsWrong() {
        assertThat(IsbnUtils.normalize("0306406153")).isNull();
    }

    @Test
    void shouldReturnNullWhenPrefixIsNotBookland() {
        assertThat(IsbnUtils.normalize("4006381333931")).isNull();
    }

    @Test
    void shouldReturnNullWhenInputIsNotAnIsbn() {
        assertThat(IsbnUtils.normalize(null)).isNull();
        assertThat(IsbnUtils.normalize("")).isNull();
        assertThat(IsbnUtils.normalize("12345")).isNull();
        assertThat(IsbnUtils.normalize("97803064061X7")).isNull();
    }
}

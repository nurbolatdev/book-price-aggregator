package kz.nurbolat.bookaggregator.parser.matching;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class NonBookFilterTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Обложка для книг прозрачная, 5 шт.",
            "Закладка магнитная «Мастер и Маргарита»",
            "Набор открыток «Москва Булгакова»",
            "Ежедневник недатированный А5",
            "Блокнот «Гарри Поттер»",
            "Карта складная Москва и Московская область (100х70см) метро и МЦК",
    })
    void shouldRejectWhenTitleHasStopWords(String title) {
        assertThat(NonBookFilter.isNonBook(title, null)).isTrue();
    }

    @Test
    void shouldRejectWhenStoreCategoryIsNotBooks() {
        assertThat(NonBookFilter.isNonBook("Мастер и Маргарита", "Канцелярские товары")).isTrue();
        assertThat(NonBookFilter.isNonBook("Мастер и Маргарита", "Аксессуары для книг")).isTrue();
    }

    @Test
    void shouldKeepBooksWhenTitleOrCategoryOnlyResemblesStopWords() {
        assertThat(NonBookFilter.isNonBook("Мастер и Маргарита (твёрдая обложка)", "Художественная литература"))
                .isFalse();
        assertThat(NonBookFilter.isNonBook("Книга джунглей", null)).isFalse();
        assertThat(NonBookFilter.isNonBook(null, null)).isFalse();
    }
}

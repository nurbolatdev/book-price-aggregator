package kz.nurbolat.bookaggregator.parser.matching;

import kz.nurbolat.bookaggregator.edition.Binding;
import kz.nurbolat.bookaggregator.parser.matching.TitleCleaner.CleanTitle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TitleCleanerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Мастер и Маргарита (твёрдая обложка)                 | Мастер и Маргарита",
            "Мастер и Маргарита. Подарочное издание               | Мастер и Маргарита",
            "Книга «Мастер и Маргарита»                           | Мастер и Маргарита",
            "Книга: Пикник на обочине                             | Пикник на обочине",
            "\"Пикник на обочине\", мягкая обложка, арт. 12345-K  | Пикник на обочине",
            "Пикник на обочине [подарочное издание] (2021)        | Пикник на обочине",
            "Пикник на обочине, 2019 г.                           | Пикник на обочине",
            "Спеши любить (эксклюзивная классика)                 | Спеши любить",
            "Война и мир. Комплект из 2 книг                      | Война и мир",
            "Шантарам — 1. Комплект из 2 книг                     | Шантарам",
            "Шантарам (в 2-х томах) (комплект)                    | Шантарам",
            "Маленький принц. С иллюстрациями автора.             | Маленький принц",
            "Мастер и Маргарита с иллюстрациями Нади Рушевой      | Мастер и Маргарита",
            "Маленький принц. Эксклюзивная классика               | Маленький принц",
            "1984 (қазақ тілінде)                                 | 1984",
            "Мастер и Маргарита. Обманщик и его маскарад (Комплект) | Мастер и Маргарита. Обманщик и его маскарад",
    })
    void shouldDropStoreAdditionsWhenTitleHasThem(String raw, String expected) {
        assertThat(TitleCleaner.clean(raw).title()).isEqualTo(expected);
    }

    @Test
    void shouldKeepTitlesThatOnlyLookLikeAdditions() {
        assertThat(TitleCleaner.clean("Книга джунглей").title()).isEqualTo("Книга джунглей");
        assertThat(TitleCleaner.clean("1984").title()).isEqualTo("1984");
        assertThat(TitleCleaner.clean("1984").year()).isNull();
    }

    @Test
    void shouldExtractBindingAndYearWhenTitleNamesThem() {
        assertThat(TitleCleaner.clean("Пикник на обочине (твердый переплет, 2021)"))
                .isEqualTo(new CleanTitle("Пикник на обочине", Binding.HARDCOVER, 2021));
        assertThat(TitleCleaner.clean("Пикник на обочине, мягкая обложка, 2019 г."))
                .isEqualTo(new CleanTitle("Пикник на обочине", Binding.PAPERBACK, 2019));
        assertThat(TitleCleaner.clean("Пикник на обочине"))
                .isEqualTo(new CleanTitle("Пикник на обочине", null, null));
    }

    @Test
    void shouldReturnNullTitleWhenTitleIsNull() {
        assertThat(TitleCleaner.clean(null).title()).isNull();
    }
}

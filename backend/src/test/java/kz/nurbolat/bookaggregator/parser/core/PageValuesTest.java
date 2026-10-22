package kz.nurbolat.bookaggregator.parser.core;

import kz.nurbolat.bookaggregator.edition.Binding;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageValuesTest {

    @Test
    void shouldReadTengeAmountsWithAnySpaces() {
        assertThat(PageValues.money("2 722 ₸")).isEqualByComparingTo("2722");
        assertThat(PageValues.money("13 850 ₸")).isEqualByComparingTo("13850");
        assertThat(PageValues.money("—")).isNull();
    }

    @Test
    void shouldReadBindingAndYearWhenTextNamesThem() {
        assertThat(PageValues.binding("мягкая обложка, 2023")).isEqualTo(Binding.PAPERBACK);
        assertThat(PageValues.binding("Твердый переплет")).isEqualTo(Binding.HARDCOVER);
        assertThat(PageValues.binding("суперобложка")).isEqualTo(Binding.UNKNOWN);
        assertThat(PageValues.year("твердый переплет, 2026")).isEqualTo(2026);
        assertThat(PageValues.year("без года")).isNull();
    }

    @Test
    void shouldTakeFirstValidIsbnOfList() {
        assertThat(PageValues.firstIsbn("978-5-9988-1721-2, 978-5-9988-1671-0")).isEqualTo("9785998817212");
        assertThat(PageValues.firstIsbn("нет")).isNull();
    }
}

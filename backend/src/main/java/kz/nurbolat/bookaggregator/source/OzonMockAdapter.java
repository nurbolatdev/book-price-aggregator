package kz.nurbolat.bookaggregator.source;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
public class OzonMockAdapter implements BookSource {

    private static final Random RANDOM = new Random();

    @Override
    public String getName() {
        return "ozon";
    }

    @Override
    public List<BookSourceResult> search(String query) {
        BigDecimal basePrice = BigDecimal.valueOf(1500 + RANDOM.nextInt(10000));
        BigDecimal originalPrice = RANDOM.nextBoolean()
                ? basePrice.multiply(BigDecimal.valueOf(1.15))
                : null;

        return List.of(
                BookSourceResult.builder()
                        .title(query)
                        .author("Автор книги")
                        .isbn(null)
                        .source(getName())
                        .price(basePrice)
                        .originalPrice(originalPrice)
                        .currency("KZT")
                        .inStock(true)
                        .url("https://ozon.ru/search/?text=" + query.replace(" ", "+"))
                        .build()
        );
    }
}

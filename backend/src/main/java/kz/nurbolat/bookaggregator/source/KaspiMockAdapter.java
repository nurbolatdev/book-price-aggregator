package kz.nurbolat.bookaggregator.source;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

@Component
public class KaspiMockAdapter implements BookSource {

    private static final Random RANDOM = new Random();

    @Override
    public String getName() {
        return "kaspi";
    }

    @Override
    public List<BookSourceResult> search(String query) {
        BigDecimal basePrice = BigDecimal.valueOf(2000 + RANDOM.nextInt(8000));
        BigDecimal originalPrice = RANDOM.nextBoolean()
                ? basePrice.multiply(BigDecimal.valueOf(1.2))
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
                        .inStock(RANDOM.nextBoolean())
                        .url("https://kaspi.kz/shop/search/?q=" + query.replace(" ", "+"))
                        .build()
        );
    }
}

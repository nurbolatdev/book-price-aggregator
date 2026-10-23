package kz.nurbolat.bookaggregator.common.text;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Prices as people read them in emails: {@code 3 150 ₸}, kopecks only when there are any ({@code 3 150,50 ₸}).
 */
public final class Money {

    private Money() {
    }

    public static String format(BigDecimal price) {
        if (price == null) {
            return null;
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        BigDecimal scaled = price.setScale(2, RoundingMode.HALF_UP);
        String pattern = scaled.stripTrailingZeros().scale() <= 0 ? "#,##0" : "#,##0.00";
        return new DecimalFormat(pattern, symbols).format(scaled) + " ₸";
    }
}

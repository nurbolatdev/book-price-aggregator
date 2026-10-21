package kz.nurbolat.bookaggregator.edition;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Price history period; written as "1M", "3M" or "6M" in requests and responses.
 */
@Getter
@RequiredArgsConstructor
public enum PricePeriod {

    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180);

    @JsonValue
    private final String code;
    private final int days;

    public static PricePeriod fromCode(String code) {
        return Arrays.stream(values())
                .filter(period -> period.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown period: " + code));
    }
}

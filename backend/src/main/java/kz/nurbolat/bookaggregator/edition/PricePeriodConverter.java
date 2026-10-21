package kz.nurbolat.bookaggregator.edition;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Binds {@code ?period=3M} request parameters.
 */
@Component
class PricePeriodConverter implements Converter<String, PricePeriod> {

    @Override
    public PricePeriod convert(String source) {
        return PricePeriod.fromCode(source.trim());
    }
}

package kz.nurbolat.bookaggregator.parser.matching;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Thresholds of matching offers without ISBN ({@code app.parser.matching.*}), see
 * docs/05-parsing-policy.md, section 3.
 *
 * @param titleThreshold  title similarity from which an offer is matched automatically
 * @param authorThreshold author similarity also required for an automatic match
 * @param reviewThreshold title similarity from which an offer waits for manual review
 *                        instead of becoming a new book
 * @param candidates      books most similar by title that are compared with an offer
 */
@Validated
@ConfigurationProperties("app.parser.matching")
public record MatchingProperties(
        @DefaultValue("0.85") @DecimalMin("0") @DecimalMax("1") double titleThreshold,
        @DefaultValue("0.7") @DecimalMin("0") @DecimalMax("1") double authorThreshold,
        @DefaultValue("0.6") @DecimalMin("0") @DecimalMax("1") double reviewThreshold,
        @DefaultValue("5") @Positive int candidates
) {
}

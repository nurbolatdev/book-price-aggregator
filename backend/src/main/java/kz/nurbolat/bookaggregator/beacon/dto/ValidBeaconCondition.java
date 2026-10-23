package kz.nurbolat.bookaggregator.beacon.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The parameter of the condition is present when its type needs it: {@code BELOW_PRICE} — {@code targetPrice},
 * {@code DISCOUNT_PERCENT} — {@code targetPercent}; a parameter without a type is an error too.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = BeaconConditionValidator.class)
public @interface ValidBeaconCondition {

    String message() default "условие маячка задано не полностью";

    /** The type may be left out (PATCH without a new condition). */
    boolean typeOptional() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

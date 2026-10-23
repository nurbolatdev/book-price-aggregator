package kz.nurbolat.bookaggregator.beacon.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

class BeaconConditionValidator implements ConstraintValidator<ValidBeaconCondition, BeaconConditionFields> {

    private boolean typeOptional;

    @Override
    public void initialize(ValidBeaconCondition annotation) {
        typeOptional = annotation.typeOptional();
    }

    @Override
    public boolean isValid(BeaconConditionFields fields, ConstraintValidatorContext context) {
        if (fields == null) {
            return true;
        }
        if (fields.conditionType() == null) {
            boolean hasParameter = fields.targetPrice() != null || fields.targetPercent() != null;
            if (typeOptional && !hasParameter) {
                return true;
            }
            return fail(context, "conditionType", "укажите тип условия");
        }
        return switch (fields.conditionType()) {
            case BELOW_PRICE -> fields.targetPrice() != null
                    || fail(context, "targetPrice", "обязательно для условия BELOW_PRICE");
            case DISCOUNT_PERCENT -> fields.targetPercent() != null
                    || fail(context, "targetPercent", "обязательно для условия DISCOUNT_PERCENT");
            case ANY_DROP -> true;
        };
    }

    private static boolean fail(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
        return false;
    }
}

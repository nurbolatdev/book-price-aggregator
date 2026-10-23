package kz.nurbolat.bookaggregator.beacon.dto;

import kz.nurbolat.bookaggregator.beacon.BeaconCondition;

import java.math.BigDecimal;

/**
 * Condition of a beacon as a request gives it, checked by {@link ValidBeaconCondition}.
 */
public interface BeaconConditionFields {

    BeaconCondition conditionType();

    BigDecimal targetPrice();

    Integer targetPercent();
}

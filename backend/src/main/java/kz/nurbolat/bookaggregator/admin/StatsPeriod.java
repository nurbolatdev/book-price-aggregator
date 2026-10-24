package kz.nurbolat.bookaggregator.admin;

import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;

import java.time.Duration;

/** Reporting period of admin queries: {@code 24h}, {@code 7d} or {@code 30d}. */
public enum StatsPeriod {
    H24("24h", Duration.ofHours(24)),
    D7("7d", Duration.ofDays(7)),
    D30("30d", Duration.ofDays(30));

    private final String code;
    private final Duration duration;

    StatsPeriod(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    public String code() {
        return code;
    }

    public Duration duration() {
        return duration;
    }

    public static StatsPeriod parse(String value) {
        for (StatsPeriod period : values()) {
            if (period.code.equals(value)) {
                return period;
            }
        }
        throw new AppException(ErrorCode.VALIDATION_FAILED, "period должен быть 24h, 7d или 30d");
    }
}

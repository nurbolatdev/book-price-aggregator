package kz.nurbolat.bookaggregator.common.error;

import java.time.Instant;
import java.util.List;

/**
 * Error body shared by all endpoints, see docs/03-api-contract.md.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        String path,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(Instant timestamp, ErrorCode code, String message, String path) {
        return of(timestamp, code, message, path, List.of());
    }

    public static ErrorResponse of(Instant timestamp, ErrorCode code, String message, String path,
                                   List<FieldError> fieldErrors) {
        return new ErrorResponse(timestamp, code.getStatus().value(), code, message, path, fieldErrors);
    }
}

package kz.nurbolat.bookaggregator.common.error;

import kz.nurbolat.bookaggregator.common.error.ErrorResponse.FieldError;
import lombok.Getter;

import java.util.List;

/**
 * Base class for business errors; the handler turns it into the API error format.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode code;
    private final List<FieldError> fieldErrors;

    public AppException(ErrorCode code, String message) {
        this(code, message, List.of());
    }

    public AppException(ErrorCode code, String message, List<FieldError> fieldErrors) {
        super(message);
        this.code = code;
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public AppException(ErrorCode code) {
        this(code, code.getDefaultMessage());
    }
}

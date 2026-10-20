package kz.nurbolat.bookaggregator.common.error;

import lombok.Getter;

/**
 * Base class for business errors; the handler turns it into the API error format.
 */
@Getter
public class AppException extends RuntimeException {

    private final ErrorCode code;

    public AppException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public AppException(ErrorCode code) {
        this(code, code.getDefaultMessage());
    }
}

package kz.nurbolat.bookaggregator.common;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ApiError {

    private final int status;
    private final String message;
    private final LocalDateTime timestamp = LocalDateTime.now();

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }
}

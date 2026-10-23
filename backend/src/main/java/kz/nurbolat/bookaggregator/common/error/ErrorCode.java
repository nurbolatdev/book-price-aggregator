package kz.nurbolat.bookaggregator.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes from the API contract, each bound to its HTTP status.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "Запрос содержит ошибки"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Ресурс не найден"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Требуется авторизация"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Доступ запрещён"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Email не подтверждён"),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "Email уже зарегистрирован"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Неверный email или пароль"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Срок действия токена истёк"),
    TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Токен недействителен"),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Слишком много запросов"),
    CONFLICT(HttpStatus.CONFLICT, "Конфликт данных"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Сервис временно недоступен"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");

    private final HttpStatus status;
    private final String defaultMessage;
}

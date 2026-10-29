package kz.nurbolat.bookaggregator.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.security.CorsProperties;
import kz.nurbolat.bookaggregator.security.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * The refresh token cookie {@code kr_refresh}: {@code HttpOnly} (scripts cannot read it), {@code SameSite=Strict},
 * and {@code Path=/api/v1/auth} so the browser sends it to the auth endpoints only.
 * A cookie is sent by the browser on its own, so a request that relies on it must come from an allowed origin.
 */
@Component
@RequiredArgsConstructor
public class RefreshCookies {

    public static final String NAME = "kr_refresh";
    private static final String PATH = "/api/v1/auth";

    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;

    /** {@code Set-Cookie} value that stores the token. */
    public String issue(String token) {
        return build(token, jwtProperties.refreshTtl()).toString();
    }

    /** {@code Set-Cookie} value that removes the cookie. */
    public String clear() {
        return build("", Duration.ZERO).toString();
    }

    /** The token from the cookie; empty if there is none or it is blank. */
    public Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Rejects a request that could be forged by another site. An {@code Origin} that is not allowed fails always;
     * a request without {@code Origin} fails when the token came from the cookie. A token in the body is not sent
     * by the browser on its own, so such a request without {@code Origin} (curl, tests) is fine.
     *
     * @throws AppException {@code FORBIDDEN}
     */
    public void checkOrigin(HttpServletRequest request, boolean fromCookie) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin == null ? fromCookie : !corsProperties.allowedOrigins().contains(origin)) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Strict")
                .path(PATH)
                .maxAge(maxAge)
                .build();
    }
}

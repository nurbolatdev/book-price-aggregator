package kz.nurbolat.bookaggregator.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads what a sign-in or refresh response hands to the client: the refresh token lives only in the cookie.
 */
public final class RefreshCookieSupport {

    private static final String PREFIX = "kr_refresh=";

    private RefreshCookieSupport() {
    }

    /** The {@code kr_refresh} value from {@code Set-Cookie}; fails when the cookie is absent or blank. */
    public static String refreshTokenOf(MockHttpServletResponse response) {
        String value = response.getHeaders(HttpHeaders.SET_COOKIE).stream()
                .filter(header -> header.startsWith(PREFIX))
                .map(header -> header.substring(PREFIX.length()).split(";", 2)[0])
                .findFirst()
                .orElse(null);
        assertThat(value).as("kr_refresh cookie").isNotBlank();
        return value;
    }

    /** {@code Bearer <access token>} taken from the response body. */
    public static String bearerOf(MockHttpServletResponse response) throws Exception {
        return "Bearer " + JsonPath.<String>read(response.getContentAsString(), "$.accessToken");
    }
}

package kz.nurbolat.bookaggregator.security.ratelimit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.common.error.ErrorResponseWriter;
import kz.nurbolat.bookaggregator.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Applies {@code app.rate-limit.*} to the API: sign-in per IP and email, registration and password reset per IP,
 * search per IP, everything else per user (per IP when signed out). Over the limit the answer is 429
 * {@code RATE_LIMITED} with {@code Retry-After}. Runs after the JWT filter, so that the user is known.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String API = "/api/";
    private static final int MAX_LOGIN_BODY = 16 * 1024;

    private final RateLimitProperties properties;
    private final RateLimiter rateLimiter;
    private final SecurityUtils securityUtils;
    private final ErrorResponseWriter errorResponseWriter;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !properties.enabled()
                || !request.getRequestURI().startsWith(API)
                || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = request.getRemoteAddr();
        boolean post = HttpMethod.POST.matches(request.getMethod());
        HttpServletRequest forwarded = request;

        String rule;
        String key;
        RateLimitProperties.Limit limit;
        if (post && path.equals("/api/v1/auth/login")) {
            // The email is in the body; it is read here and replayed for the controller
            CachedBodyRequest cached = CachedBodyRequest.of(request, MAX_LOGIN_BODY);
            forwarded = cached;
            rule = "login";
            key = ip + ":" + email(cached.body());
            limit = properties.login();
        } else if (post && path.equals("/api/v1/auth/register")) {
            rule = "register";
            key = ip;
            limit = properties.register();
        } else if (post && path.equals("/api/v1/auth/forgot-password")) {
            rule = "forgot-password";
            key = ip;
            limit = properties.forgotPassword();
        } else if (HttpMethod.GET.matches(request.getMethod()) && path.equals("/api/v1/search")) {
            rule = "search";
            key = ip;
            limit = properties.search();
        } else if (HttpMethod.GET.matches(request.getMethod()) && path.equals("/api/v1/search/facets")) {
            // The UI asks for search and facets together; separate buckets keep the search budget intact
            rule = "facets";
            key = ip;
            limit = properties.search();
        } else {
            rule = "api";
            key = securityUtils.currentUser().map(user -> "user:" + user.id()).orElse("ip:" + ip);
            limit = properties.api();
        }

        long retryAfter = rateLimiter.tryConsume(rule + ":" + key, limit);
        if (retryAfter > 0) {
            log.info("Rate limit {} exceeded on {} {}, retry after {} s", rule, request.getMethod(), path, retryAfter);
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter));
            errorResponseWriter.write(request, response, ErrorCode.RATE_LIMITED);
            return;
        }
        filterChain.doFilter(forwarded, response);
    }

    private String email(byte[] body) {
        if (body.length == 0) {
            return "";
        }
        try {
            JsonNode email = objectMapper.readTree(body).path("email");
            return email.isTextual() ? email.asText().trim().toLowerCase(Locale.ROOT) : "";
        } catch (IOException e) {
            // Not JSON: the controller answers 4xx, the attempt still counts against the IP
            return "";
        }
    }

    /**
     * Request whose body was read up front (at most {@code limit} bytes, the rest stays in the stream) and is
     * given again to whoever reads it next.
     */
    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;
        private final ServletInputStream input;

        private CachedBodyRequest(HttpServletRequest request, byte[] body, InputStream input) {
            super(request);
            this.body = body;
            this.input = new ServletInputStream() {
                private boolean finished;

                @Override
                public int read() throws IOException {
                    int b = input.read();
                    finished = b == -1;
                    return b;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int n = input.read(b, off, len);
                    finished = n == -1;
                    return n;
                }

                @Override
                public boolean isFinished() {
                    return finished;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {
                    throw new UnsupportedOperationException("Asynchronous reading is not supported");
                }
            };
        }

        static CachedBodyRequest of(HttpServletRequest request, int limit) throws IOException {
            ServletInputStream original = request.getInputStream();
            byte[] head = original.readNBytes(limit);
            InputStream replay = new SequenceInputStream(new ByteArrayInputStream(head), original);
            return new CachedBodyRequest(request, head, replay);
        }

        byte[] body() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return input;
        }

        @Override
        public BufferedReader getReader() {
            Charset charset = getCharacterEncoding() == null ? StandardCharsets.UTF_8 : Charset.forName(getCharacterEncoding());
            return new BufferedReader(new InputStreamReader(input, charset));
        }
    }
}

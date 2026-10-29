package kz.nurbolat.bookaggregator.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kz.nurbolat.bookaggregator.user.User;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Access tokens: signed JWT with the user id ({@code sub}), {@code email}, {@code role} and the session ({@code sid}).
 */
@Service
public class JwtService {

    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";
    private static final String SESSION_CLAIM = "sid";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey key;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        return generateAccessToken(user, null);
    }

    /**
     * @param sessionId refresh token family of the sign-in; lets the server tell which session a request belongs to
     *                  without the refresh cookie, which the browser sends to {@code /auth} only
     */
    public String generateAccessToken(User user, UUID sessionId) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(SESSION_CLAIM, sessionId == null ? null : sessionId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTtl())))
                .signWith(key)
                .compact();
    }

    /**
     * @throws ExpiredJwtException the token was valid but has expired
     * @throws JwtException        the token is malformed or its signature is wrong
     */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        try {
            String sid = claims.get(SESSION_CLAIM, String.class);
            return new AuthenticatedUser(Long.valueOf(claims.getSubject()), claims.get(EMAIL_CLAIM, String.class),
                    User.Role.valueOf(claims.get(ROLE_CLAIM, String.class)), sid == null ? null : UUID.fromString(sid));
        } catch (IllegalArgumentException | NullPointerException e) {
            // Signed by us but without the claims of this version (tokens issued before M18)
            throw new JwtException("Token without user claims", e);
        }
    }

    public long accessTtlSeconds() {
        return properties.accessTtl().toSeconds();
    }
}

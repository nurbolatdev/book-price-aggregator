package kz.nurbolat.bookaggregator.security;

import kz.nurbolat.bookaggregator.user.User;
import org.springframework.security.core.AuthenticatedPrincipal;

import java.util.UUID;

/**
 * The signed-in user as the access token describes it, without a database lookup.
 * {@code sessionId} is the refresh token family the token was issued for; null for older tokens.
 */
public record AuthenticatedUser(Long id, String email, User.Role role, UUID sessionId) implements AuthenticatedPrincipal {

    /** Without a session: access tokens issued before the {@code sid} claim existed. */
    public AuthenticatedUser(Long id, String email, User.Role role) {
        this(id, email, role, null);
    }

    @Override
    public String getName() {
        return email;
    }
}

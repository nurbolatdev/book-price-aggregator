package kz.nurbolat.bookaggregator.security;

import kz.nurbolat.bookaggregator.user.User;
import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * The signed-in user as the access token describes it, without a database lookup.
 */
public record AuthenticatedUser(Long id, String email, User.Role role) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return email;
    }
}

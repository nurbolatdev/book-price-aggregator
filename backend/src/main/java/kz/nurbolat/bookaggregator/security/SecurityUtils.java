package kz.nurbolat.bookaggregator.security;

import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.user.User;
import kz.nurbolat.bookaggregator.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * The signed-in user from the access token, without touching the database; empty for anonymous requests.
     */
    public Optional<AuthenticatedUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * Email of the signed-in user without touching the database; empty for anonymous requests.
     */
    public Optional<String> currentUserEmail() {
        return currentUser().map(AuthenticatedUser::email);
    }

    /**
     * @throws AppException {@code UNAUTHORIZED} if nobody is signed in or the account was deleted meanwhile
     */
    public User getCurrentUser() {
        return currentUser()
                .flatMap(user -> userRepository.findById(user.id()))
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED));
    }
}

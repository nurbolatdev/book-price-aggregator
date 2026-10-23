package kz.nurbolat.bookaggregator.user.dto;

import kz.nurbolat.bookaggregator.user.NotifyFrequency;
import kz.nurbolat.bookaggregator.user.User;

/**
 * The user's own profile: in the sign-in response and at {@code GET /me}.
 */
public record UserResponse(Long id, String email, String name, User.Role role, boolean emailVerified,
                           NotifyFrequency notifyFrequency, boolean weeklyDigest) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.isEmailVerified(),
                user.getNotifyFrequency(), user.isWeeklyDigest());
    }
}

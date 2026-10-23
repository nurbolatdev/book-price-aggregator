package kz.nurbolat.bookaggregator.user.dto;

import kz.nurbolat.bookaggregator.user.User;

public record UserResponse(Long id, String email, String name, User.Role role, boolean emailVerified) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.isEmailVerified());
    }
}

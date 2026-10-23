package kz.nurbolat.bookaggregator.user.dto;

/**
 * @param expiresIn seconds the access token lives
 */
public record AuthResponse(String accessToken, String refreshToken, long expiresIn, UserResponse user) {
}

package kz.nurbolat.bookaggregator.user.dto;

/**
 * Body of sign-in and refresh; the refresh token travels only in the {@code kr_refresh} cookie.
 *
 * @param expiresIn seconds the access token lives
 */
public record AuthResponse(
        String accessToken,
        long expiresIn,
        UserResponse user) {
}

package kz.nurbolat.bookaggregator.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @param refreshToken also sent in the {@code kr_refresh} cookie; the body field stays until the frontend moves to the cookie
 * @param expiresIn    seconds the access token lives
 */
public record AuthResponse(
        String accessToken,
        @Schema(description = "Refresh-токен; дублируется в cookie kr_refresh. Устарело: убрать в F4", deprecated = true)
        String refreshToken,
        long expiresIn,
        UserResponse user) {
}

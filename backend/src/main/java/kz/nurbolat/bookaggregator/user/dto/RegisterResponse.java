package kz.nurbolat.bookaggregator.user.dto;

/**
 * @param emailVerificationRequired always {@code true}: a confirmation link was sent
 */
public record RegisterResponse(Long userId, boolean emailVerificationRequired) {
}

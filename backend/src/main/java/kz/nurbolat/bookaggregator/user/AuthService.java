package kz.nurbolat.bookaggregator.user;

import kz.nurbolat.bookaggregator.auth.EmailVerificationService;
import kz.nurbolat.bookaggregator.auth.RefreshTokenService;
import kz.nurbolat.bookaggregator.common.error.AppException;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.security.JwtService;
import kz.nurbolat.bookaggregator.user.dto.AuthResponse;
import kz.nurbolat.bookaggregator.user.dto.LoginRequest;
import kz.nurbolat.bookaggregator.user.dto.RegisterRequest;
import kz.nurbolat.bookaggregator.user.dto.RegisterResponse;
import kz.nurbolat.bookaggregator.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;

    /**
     * Creates an account with an unconfirmed email and sends the confirmation link. The user signs in separately.
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_TAKEN);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        userRepository.save(user);
        emailVerificationService.sendLink(user);

        return new RegisterResponse(user.getId(), true);
    }

    /**
     * Signing in does not need a confirmed email; the response tells the client whether it is confirmed.
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String userAgent) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return tokens(user, refreshTokenService.issue(user, userAgent));
    }

    /**
     * New access token and the next refresh token; the one used is revoked.
     */
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse refresh(String refreshToken, String userAgent) {
        RefreshTokenService.Rotated rotated = refreshTokenService.rotate(refreshToken, userAgent);
        return tokens(rotated.user(), rotated.token());
    }

    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private AuthResponse tokens(User user, String refreshToken) {
        return new AuthResponse(jwtService.generateAccessToken(user), refreshToken, jwtService.accessTtlSeconds(),
                UserResponse.from(user));
    }
}

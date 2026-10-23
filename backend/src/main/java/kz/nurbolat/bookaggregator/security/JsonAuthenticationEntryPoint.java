package kz.nurbolat.bookaggregator.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.nurbolat.bookaggregator.common.error.ErrorCode;
import kz.nurbolat.bookaggregator.common.error.ErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        boolean expired = request.getAttribute(JwtAuthFilter.TOKEN_EXPIRED_ATTRIBUTE) != null;
        errorResponseWriter.write(request, response, expired ? ErrorCode.TOKEN_EXPIRED : ErrorCode.UNAUTHORIZED);
    }
}

package kz.nurbolat.bookaggregator.common.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

/**
 * Writes the API error format directly to the servlet response,
 * for errors raised outside of controllers (Spring Security filters).
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        ErrorResponse body = ErrorResponse.of(clock.instant(), code, code.getDefaultMessage(), request.getRequestURI());
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

package com.chethu.paymentledgerservice.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.chethu.paymentledgerservice.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    public JwtAuthenticationEntryPoint() {
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write(toJson(new ErrorResponse(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Authentication required",
                request.getRequestURI())));
    }

    private String toJson(ErrorResponse errorResponse) {
        return """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s"}
                """
                .formatted(
                        escape(errorResponse.getTimestamp().toString()),
                        errorResponse.getStatus(),
                        escape(errorResponse.getError()),
                        escape(errorResponse.getMessage()),
                        escape(errorResponse.getPath()))
                .trim();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

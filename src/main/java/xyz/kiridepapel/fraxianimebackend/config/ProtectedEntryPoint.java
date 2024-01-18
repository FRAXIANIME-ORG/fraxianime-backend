package xyz.kiridepapel.fraxianimebackend.config;

import java.io.IOException;

import org.springframework.security.web.AuthenticationEntryPoint;

import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;

@Component
public class ProtectedEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
        AuthenticationException authException) throws IOException {
        if (!request.getRequestURI().startsWith("/api/v1/")) {
            ResponseDTO responseDTO = new ResponseDTO("Acceso denegado", 401);
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"message\":\"" + responseDTO.getMessage() +
                "\",\"status\":" + responseDTO.getStatus() +"}");
            response.getWriter().flush();
        }
    }
}
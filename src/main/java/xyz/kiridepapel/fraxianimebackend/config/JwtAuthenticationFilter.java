package xyz.kiridepapel.fraxianimebackend.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.utils.JwtUtils;

@Component
@SuppressWarnings("all")
@Log
public class JwtAuthenticationFilter extends OncePerRequestFilter{
  @Autowired
  private UserDetailsService userDetailsService;
  @Autowired
  private JwtUtils jwtUtils;

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // log.info("si");
    
    try {
      final String username;
      final String token = jwtUtils.getTokenFromRequest(request);
      
      // Validar el token cuando no esta iniciando sesion
      if (token == null && 
          request.getRequestURI().startsWith("/api/v1/auth") ||
          request.getRequestURI().startsWith("/api/v1/anime")
      ) {
        filterChain.doFilter(request, response);
        return;
      } else if (token == null) {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "El token es invalido");
        return;
      }

      // Validar si el token esta en la lista negra
      if (jwtUtils.isTokenBlacklisted(token)) {
        sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "El token es invalido");
        return;
      }

      // Obtener el usuario del token
      username = jwtUtils.getUsernameFromToken(token);

      // Autenticar el usuario
      // Si el usuario existe y no esta autenticado
      if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        // Si el token es valido
        if (jwtUtils.isTokenValid(token, userDetails)) {
          // Autenticar el usuario
          UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities()
          );
          // Agregar los detalles de la peticion a la autenticacion
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          // Autenticar el usuario en el contexto de seguridad de Spring
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      }

      filterChain.doFilter(request, response);
    } catch (JwtException e) {
      sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "El token es invalido");
    }
  }

  
  private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message)
      throws IOException, io.jsonwebtoken.io.IOException {
    response.setStatus(status.value());
    response.setContentType("application/json");
    response.getWriter().write("{\"message\":\"" + message + "\"}");
    response.getWriter().flush();
  }
}

package xyz.kiridepapel.fraxianimebackend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.service.AuthService;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;
import xyz.kiridepapel.fraxianimebackend.utils.JwtUtils;

@RestController
@RequestMapping("/api/v1/user")
@CrossOrigin(
  origins = {
    "https://fraxianime.vercel.app",
    "http://localhost:4200",
  }, allowedHeaders = "**")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
@SuppressWarnings("unused")
public class UserController {
  // Variables estaticas
  @Value("${FRONTEND_URL}")
  private String frontendUrl;
  // Variables
  private List<String> allowedOrigins;
  // Inyección de dependencias
  @Autowired
  private AuthService authService;
  @Autowired
  private JwtUtils jwtUtils;
  
  @PostConstruct
  public void init() {
    this.allowedOrigins = List.of(frontendUrl);
  }

  @PostMapping("/logout")
  public ResponseEntity<ResponseDTO> logout(HttpServletRequest request) {
    // Validaciones
    // DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));

    // Procesar la solicitud y construir la respuesta
    String realToken = jwtUtils.getTokenFromRequest(request);
    ResponseDTO response = authService.logout(realToken);
    return new ResponseEntity<ResponseDTO>(response, HttpStatus.valueOf(response.getStatus()));
  }
}

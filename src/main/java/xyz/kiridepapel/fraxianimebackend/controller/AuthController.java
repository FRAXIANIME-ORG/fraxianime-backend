package xyz.kiridepapel.fraxianimebackend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.java.Log;
import xyz.kiridepapel.fraxianimebackend.dto.AuthRequestDTO;
import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.ArgumentRequiredException;
import xyz.kiridepapel.fraxianimebackend.service.AuthService;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(
  origins = {
    "https://fraxianime.vercel.app",
    "http://localhost:4200",
  }, allowedHeaders = "**")
@SuppressWarnings("unused")
@Log
public class AuthController {
  // Variables estaticas
  @Value("${FRONTEND_URL}")
  private String frontendUrl;
  // Variables
  private List<String> allowedOrigins;
  // Inyección de dependencias
  @Autowired
  private AuthService authService;

  @PostConstruct
  public void init() {
    this.allowedOrigins = List.of(frontendUrl);
  }

  @PostMapping("/register")
  public ResponseEntity<?> register(HttpServletRequest request, @RequestBody(required = true) AuthRequestDTO data) {
    // Validaciones    
    // DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    this.isValidEmail(data.getEmail());
    this.isValidPassword(data.getPassword());
    DataUtils.verifySQLInjection(data.getEmail());
    DataUtils.verifySQLInjection(data.getPassword());

    // ResponseDTO response = authService.register(data);
    // Integer code = response.getCode();
    Map<String, Object> response = new HashMap<>() {{
      put("code", 400);
      put("message", "Registro deshabilitado");
    }};
    Integer code = (Integer) response.get("code");

    return new ResponseEntity<>(response, HttpStatus.valueOf(code));
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(HttpServletRequest request, @RequestBody(required = true) AuthRequestDTO data) {
    // Validaciones
    // DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    this.isValidEmail(data.getEmail());
    this.isValidPassword(data.getPassword());
    DataUtils.verifySQLInjection(data.getEmail());
    DataUtils.verifySQLInjection(data.getPassword());

    ResponseDTO response = authService.login(data);
    Integer code = response.getCode();
    
    return new ResponseEntity<>(response, HttpStatus.valueOf(code));
  }

  public void isValidEmail(String str) {
    if (str == null || str.isEmpty()) {
      throw new ArgumentRequiredException("El correo electrónico es obligatorio");
    }
    if (!str.contains("@") || !str.contains(".")) {
      throw new ArgumentRequiredException("El correo electrónico no es válido");
    }
  }

  public void isValidPassword(String str) {
    if (str == null || str.isEmpty()) {
      throw new ArgumentRequiredException("La contraseña es obligatoria");
    }
    if (str.length() < 6) {
      throw new ArgumentRequiredException("La contraseña es muy corta");
    }
  }
}

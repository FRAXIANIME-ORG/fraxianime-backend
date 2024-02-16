package xyz.kiridepapel.fraxianimebackend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
  @Value("${APP_SECRET}")
  private String appSecret;
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
  public ResponseEntity<ResponseDTO> register(
      HttpServletRequest request, @RequestBody(required = true) AuthRequestDTO data) {
    // Validaciones
    // DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.isValidStr(data.getUsername(), "El nombre de usuario es obligatorio");
    DataUtils.isValidStr(data.getPassword(), "La contraseña es obligatoria");
    DataUtils.verifySQLInjection(data.getUsername());
    DataUtils.verifySQLInjection(data.getPassword());

    return new ResponseEntity<>(authService.register(data), HttpStatus.CREATED);
  }

  @PostMapping("/login")
  public ResponseEntity<ResponseDTO> login(
      HttpServletRequest request, @RequestBody(required = true) AuthRequestDTO data) {
    // Validaciones
    // DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.isValidStr(data.getUsername(), "El nombre de usuario es obligatorio");
    DataUtils.isValidStr(data.getPassword(), "La contraseña es obligatoria");
    DataUtils.verifySQLInjection(data.getUsername());
    DataUtils.verifySQLInjection(data.getPassword());

    return new ResponseEntity<>(authService.login(data), HttpStatus.OK);
  }

}

package xyz.kiridepapel.fraxianimebackend.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import xyz.kiridepapel.fraxianimebackend.dtos.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.interfaces.IMegaService;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

@RestController
@RequestMapping("/api/v1/mega")
@CrossOrigin(
  origins = {
    "https://fraxianime.vercel.app",
    "https://heatheranime.vercel.app",
    "http://localhost:4200",
  }, allowedHeaders = "**")
@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
public class MegaController {
  // Variables estaticas
  @Value("${FRONTEND_URL1}")
  private String frontendUrl1;
  @Value("${FRONTEND_URL2}")
  private String frontendUrl2;
  // Variables
  private List<String> allowedOrigins;
  // Inyección de dependencias
  private final IMegaService iMegaService;

  // Constructor
  public MegaController(IMegaService iMegaService) {
    this.iMegaService = iMegaService;
  }
  
  // Inicialización
  @PostConstruct
  public void init() {
    this.allowedOrigins = List.of(frontendUrl1, frontendUrl2);
  }

  @GetMapping("/validate")
  public ResponseEntity<?> isValidMegaLink(HttpServletRequest request, @RequestBody LinkDTO data) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    DataUtils.isValidStr(data.getUrl(), "La URL es obligatoria");
    DataUtils.verifySQLInjection(data.getUrl());

    Boolean response = this.iMegaService.isValidMegaLink(data.getUrl());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}

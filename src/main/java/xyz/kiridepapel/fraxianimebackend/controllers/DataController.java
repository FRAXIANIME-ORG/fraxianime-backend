package xyz.kiridepapel.fraxianimebackend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import xyz.kiridepapel.fraxianimebackend.dtos.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.SpecialCaseDTO;
import xyz.kiridepapel.fraxianimebackend.exceptions.DataExceptions.DataNotFoundException;
import xyz.kiridepapel.fraxianimebackend.interfaces.IDataService;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/v1/data")
@CrossOrigin(
  origins = {
    "https://fraxianime.vercel.app",
    "https://heatheranime.vercel.app",
    "http://localhost:4200",
  }, allowedHeaders = "**")
public class DataController {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${FRONTEND_URL1}")
  private String frontendUrl1;
  @Value("${FRONTEND_URL2}")
  private String frontendUrl2;
  // Variables
  private List<String> allowedOrigins;
  private List<String> allowedDataNames;
  // Inyeccion de dependencias
  private final IDataService<?> iDataService;

  // Constructor
  public DataController(IDataService<?> iDataService) {
    this.iDataService = iDataService;
  }

  // Inicialización
  @PostConstruct
  private void init() {
    this.allowedOrigins = List.of(frontendUrl1, frontendUrl2);
    this.allowedDataNames = List.of(
      "translations",
      "specialCases"
    );
  }
  
  // * Export and Import data
  @GetMapping("/{dataName}/export")
  public ResponseEntity<byte[]> export(
      HttpServletRequest request,  @PathVariable("dataName") String dataName)  {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    if (!this.allowedDataNames.contains(dataName)) {
      throw new DataNotFoundException("No existen acciones para el recurso solicitado");
    }
    
    byte[] excelBytes = this.iDataService.exportExcel(dataName);
    String fileName = this.createFileName(dataName);

    // Crear respuesta con el archivo creado
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDispositionFormData("filename", fileName);
    headers.setContentLength(excelBytes.length);

    return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
  }

  @PostMapping("/{dataName}/import")
  public ResponseEntity<?> importFromExcel(HttpServletRequest request,
      MultipartFile file, @PathVariable("dataName") String dataName) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    if (!this.allowedDataNames.contains(dataName)) {
      throw new DataNotFoundException("No existen acciones para el recurso solicitado");
    }
    
    try {
      // Importar datos
      byte[] bytes = file.getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
      this.iDataService.importExcel(dataName, inputStream);

      // Normalizar el nombre del tipo de datos
      dataName = DataUtils.formatToNormalName(dataName);
      // Crear mensaje de respuesta
      ResponseDTO response = ResponseDTO.builder()
        .message("Los datos de '" + dataName + "' han sido importados correctamente")
        .code(200)
        .build();
      
      return new ResponseEntity<>(response, HttpStatus.valueOf(response.getCode()));
    } catch (IOException e) {
      ResponseDTO response = ResponseDTO.builder()
        .message("Error al procesar el archivo Excel")
        .code(500)
        .build();
      
      return new ResponseEntity<>(response, HttpStatus.valueOf(response.getCode()));
    }
  }

  // * Special cases
  @PostMapping("/new-special-case")
  public ResponseEntity<?> newSpecialCase(HttpServletRequest request,
      @RequestBody(required = true) SpecialCaseDTO data) {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    this.validateSpecialCasesData(data);

    // Crear caso especial
    this.iDataService.newSpecialCase(data);
    
    // Variables
    String msg = "El caso especial ha sido creado correctamente";
    return new ResponseEntity<>(new ResponseDTO(msg, 200), HttpStatus.OK);
  }

  // * Update cache
  @GetMapping("/update-top-cache")
  public ResponseEntity<?> scheduleUpdateCache(HttpServletRequest request) {
    // Validaciones
    // DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    // Actualizar cache
    this.iDataService.updateTop();
    // Respuesta
    return new ResponseEntity<>("Cache de top actualizado correctamente", HttpStatus.OK);
  }

  // * Methods
  private String createFileName(String dataName) {
    LocalDateTime now = DataUtils.getLocalDateTimeNow(isProduction);
    String dateTime =
      "(" + String.format("%02d", now.getDayOfMonth()) + "-" + String.format("%02d", now.getMonthValue()) + "-" + now.getYear() + ") (" +
      String.format("%02d", now.getHour()) + "-" + String.format("%02d", now.getMinute()) + ")";
    // Nombre del archivo
    return DataUtils.formatToNormalName(dataName) + " " + dateTime + ".xlsx";
  }

  private void validateSpecialCasesData(SpecialCaseDTO data) {
    // Verificación de SQL Injection
    DataUtils.verifySQLInjection(data.getJkName());
    DataUtils.verifySQLInjection(data.getLfName());
    DataUtils.verifySQLInjection(data.getJkUrl());
    DataUtils.verifySQLInjection(data.getLfUrl());

    // Validacion de escenarios
    if (data.getType() == null || data.getType() < 1 && data.getType() > 2) {
      throw new DataNotFoundException("El tipo de caso especial es obligatorio");
    }
    if (data.getChangeName() == true) {
      DataUtils.isValidStr(data.getJkName(), "El nombre del ánime en JkAnime es obligatorio");
    }
    if (data.getChangeName() == true) {
      DataUtils.isValidStr(data.getLfName(), "El nombre del ánime en AnimeLife es obligatorio");
    }
    if (data.getChangeUrlAnime() == true || data.getChangeUrlChapter()) {
      DataUtils.isValidStr(data.getJkUrl(), "La URL del ánime en JkAnime es obligatoria");
    }
    if (data.getChangeUrlAnime() == true || data.getChangeUrlChapter()) {
      DataUtils.isValidStr(data.getLfUrl(), "La URL del ánime en AnimeLife es obligatoria");
    }
  }
}

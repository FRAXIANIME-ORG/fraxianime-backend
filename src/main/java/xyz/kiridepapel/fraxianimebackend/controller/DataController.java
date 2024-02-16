package xyz.kiridepapel.fraxianimebackend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.DataNotFoundException;
import xyz.kiridepapel.fraxianimebackend.service.DataService;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/data")
@CrossOrigin(
  origins = {
    "https://fraxianime.vercel.app",
    "http://localhost:4200",
  }, allowedHeaders = "**")
@PreAuthorize("hasAnyRole('ADMIN')")
public class DataController {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${APP_SECRET}")
  private String appSecret;
  @Value("${FRONTEND_URL}")
  private String frontendUrl;
  // Variables
  private List<String> allowedOrigins;
  private List<String> allowedDataNames;
  // Inyeccion de dependencias
  @Autowired
  private DataService<?> dataService;

  @PostConstruct
  public void init() {
    this.allowedOrigins = List.of(frontendUrl);
    this.allowedDataNames = List.of("translations", "specialCases");
  }

  @GetMapping("/{dataName}/export")
  public ResponseEntity<byte[]> export(
      HttpServletRequest request,  @PathVariable("dataName") String dataName)  {
    // Validaciones
    DataUtils.verifyAllowedOrigin(this.allowedOrigins, request.getHeader("Origin"));
    if (!this.allowedDataNames.contains(dataName)) {
      throw new DataNotFoundException("No existen acciones para el recurso solicitado");
    }

    // Crea y asigna los datos a exportar en base al nombre de la tabla en la base de datos.
    byte[] excelBytes = new byte[0];
    excelBytes = this.dataService.exportExcel(dataName);

    // Fecha y hora actual
    String fileName = this.createFileName(dataName);

    // Encabezados de la respuesta
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
      // Variables
      byte[] bytes = file.getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);

      // Importar datos en base al nombre de la tabla en la base de datos.
      // Disponibles: translations, specialCases
      this.dataService.importExcel(dataName, inputStream);

      dataName = DataUtils.formatToNormalName(dataName);
      
      // Responder
      String msg = "Los datos de '" + dataName + "' han sido importados correctamente";
      return new ResponseEntity<>(new ResponseDTO(msg, 200), HttpStatus.OK);
    } catch (IOException e) {
        ResponseDTO rps = ResponseDTO.builder()
          .message("Error al procesar el archivo Excel").status(500).build();
        return new ResponseEntity<>(rps.getMessage(), HttpStatus.valueOf(rps.getStatus()));
    }
  }

  @GetMapping("/")

  private String createFileName(String dataName) {
    LocalDateTime now = DataUtils.getLocalDateTimeNow(isProduction);
    String dateTime =
      "(" + String.format("%02d", now.getDayOfMonth()) + "-" + String.format("%02d", now.getMonthValue()) + "-" + now.getYear() + ") (" +
      String.format("%02d", now.getHour()) + "-" + String.format("%02d", now.getMinute()) + ")";
    // Nombre del archivo
    return DataUtils.formatToNormalName(dataName) + " " + dateTime + ".xlsx";
  }

}

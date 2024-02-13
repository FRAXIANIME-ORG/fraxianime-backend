package xyz.kiridepapel.fraxianimebackend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.ProtectedResource;
import xyz.kiridepapel.fraxianimebackend.service.TranslateService;
import xyz.kiridepapel.fraxianimebackend.utils.DataUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/api/v1/translate")
public class TranslateController {
  // Variables estaticas
  @Value("${APP_PRODUCTION}")
  private Boolean isProduction;
  @Value("${APP_SECRET}")
  private String appSecret;
  // Inyeccion de dependencias
  @Autowired
  private TranslateService translateService;

  @GetMapping("/export/{token}")
  public ResponseEntity<byte[]> export(@PathVariable("token") String token)  {
    this.validateToken(token);
    
    byte[] excelBytes = this.translateService.databaseToExcel();
    LocalDateTime now = DataUtils.getLocalDateTimeNow(isProduction);

    String dateTime =
      "(" + String.format("%02d", now.getDayOfMonth()) + "-" + String.format("%02d", now.getMonthValue()) + "-" + now.getYear() + ") (" +
      String.format("%02d", now.getHour()) + "-" + String.format("%02d", now.getMinute()) + ")";
    String fileame = "Translations " + dateTime + ".xlsx";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    headers.setContentDispositionFormData("filename", fileame);
    headers.setContentLength(excelBytes.length);

    return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
  }

  @PostMapping("/import/{token}")
  public ResponseEntity<?> importFromExcel(@PathVariable("token") String token, MultipartFile file) {
    this.validateToken(token);

    try {
        byte[] bytes = file.getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        
        String msg = this.translateService.excelToDatabase(inputStream);

        ResponseDTO rps = ResponseDTO.builder()
          .message(msg).status(200).build();
        return new ResponseEntity<>(rps.getMessage(), HttpStatus.valueOf(rps.getStatus()));
    } catch (IOException e) {
        ResponseDTO rps = ResponseDTO.builder()
          .message("Error al procesar el archivo Excel").status(500).build();
        return new ResponseEntity<>(rps.getMessage(), HttpStatus.valueOf(rps.getStatus()));
    }
  }

  private void validateToken(String token) {
    if (this.appSecret.equals(token)) {
      return;
    } else {
      throw new ProtectedResource("Acceso denegado");
    }
  }

}

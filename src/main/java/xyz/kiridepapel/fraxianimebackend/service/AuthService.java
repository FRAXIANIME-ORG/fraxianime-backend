package xyz.kiridepapel.fraxianimebackend.service;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import xyz.kiridepapel.fraxianimebackend.dto.AuthRequestDTO;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class AuthService {
  // Variables
  @Value("${FIREBASE_WEP_API_KEY}")
  private String firebaseWebApiKey;
  @Value("${APP_PRODUCTION}")
  private Boolean appProduction;
  private String firebaseSignInUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=";
  private String firebaseSignUpUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=";
  // Inicialización de variables
  @PostConstruct
  public void init() {
    this.firebaseSignInUrl = firebaseSignInUrl + firebaseWebApiKey;
    this.firebaseSignUpUrl = firebaseSignUpUrl + firebaseWebApiKey;
  }

  // Registra un nuevo usuario
  public Map<String, Object> register(AuthRequestDTO data) {
    // Armar el cuerpo de la solicitud
    Map<String, Object> requestBody = Map.of(
      "email", data.getEmail(),
      "password", data.getPassword(),
      "returnSecureToken", true
    );

    // Enviar la solicitud y obtener la respuesta
    Map<String, Object> response = sendRequestAndGetResponse(firebaseSignUpUrl, requestBody);
    response = validateErrorRegisterMsgs(response);

    // Retornar la respuesta
    return response;
  }
  
  // Inicia sesión con una cuenta existente
  public Map<String, Object> login(AuthRequestDTO data) {
    // Armar el cuerpo de la solicitud
    Map<String, Object> requestBody = Map.of(
      "email", data.getEmail(),
      "password", data.getPassword(),
      "returnSecureToken", true
    );

    // Enviar la solicitud y obtener la respuesta
    Map<String, Object> response = sendRequestAndGetResponse(firebaseSignInUrl, requestBody);
    response = validateErrorLoginMsgs(response);
    
    // Retornar la respuesta
    return response;
  }

  // Realiza la solicitud y obtiene la respuesta
  private Map<String, Object> sendRequestAndGetResponse(String firebaseUrl, Map<String, Object> requestBody) {
    try {
      // Arma la solicitud
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

      // Realizar la solicitud
      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity<Map> response = restTemplate.postForEntity(firebaseUrl, request, Map.class);
      Map<String, String> responseBody = response.getBody();

      Integer code = response.getStatusCode().value();
      String token = responseBody.get("idToken");

      return new HashMap<String, Object>() {{
        put("code", code);
        put("token", token);
      }};
    } catch (HttpClientErrorException e) {
      String jsonResponse = convertEOLtoJsonStr(e.getMessage());
      JSONObject jsonObject = new JSONObject(jsonResponse);
      JSONObject errorObject = jsonObject.getJSONObject("error");

      Integer code = errorObject.getInt("code");
      String message = errorObject.getString("message");

      return new HashMap<String, Object>() {{
        put("code", code);
        put("message", message);
      }};
    } catch (Exception e) {
      Integer code = 500;
      String message = e.getMessage();
      
      return new HashMap<String, Object>() {{
        put("code", code);
        put("message", message);
      }};
    }

  }

  // Valida los mensajes de error al registrarse
  private Map<String, Object> validateErrorRegisterMsgs(Map<String, Object> response) {
    // Si el código no empieza con 2, es un error
    if (!response.get("code").toString().startsWith("2")) {
      String personalizedMsg = (String) response.get("message");

      // Modica el mensaje de error
      if (personalizedMsg.contains("WEAK_PASSWORD")) {
        personalizedMsg = "La contraseña es muy débil";
      } else if (personalizedMsg.equals("EMAIL_EXISTS")) {
        personalizedMsg = "El correo electrónico ya está en uso";
      } else if (personalizedMsg.equals("INVALID_EMAIL")) {
        personalizedMsg = "El correo electrónico no es válido";
      }
      
      response.put("message", personalizedMsg);
    }

    return response;
  }

  // Valida los mensajes de error al iniciar sesión
  private Map<String, Object> validateErrorLoginMsgs(Map<String, Object> response) {
    // Si el código no empieza con 2, es un error
    if (!response.get("code").toString().startsWith("2")) {
      String personalizedMsg = (String) response.get("message");

      // Modica el mensaje de error
      if (personalizedMsg.equals("INVALID_LOGIN_CREDENTIALS")) {
        personalizedMsg = "Credenciales inválidas";
      } else if (personalizedMsg.equals("INVALID_EMAIL")) {
        personalizedMsg = "El correo electrónico no es válido";
      } else if (personalizedMsg.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) {
        personalizedMsg = "Demasiados intentos";
      }
      
      response.put("message", personalizedMsg);
    }

    return response;
  }

  // Convierte la respuesta de EOL a JSON
  public static String convertEOLtoJsonStr(String eolResponseStr) {      
    // Remover caracteres de salto de línea y comillas adicionales
    String cleanedResponse = eolResponseStr
      .replaceAll("<EOL>", "")
      .replaceAll("\"\"", "\"");

    // Extraer el JSON de la cadena
    int startIndex = cleanedResponse.indexOf("{");
    int endIndex = cleanedResponse.lastIndexOf("}");
    String jsonResponse = cleanedResponse.substring(startIndex, endIndex + 1);

    return jsonResponse;
  }
}

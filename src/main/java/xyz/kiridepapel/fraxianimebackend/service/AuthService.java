package xyz.kiridepapel.fraxianimebackend.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.dto.AuthRequestDTO;
import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;

@Service
@SuppressWarnings("all")
public class AuthService {
  @Value("${FIREBASE_WEP_API_KEY}")
  private String firebaseWebApiKey;

  public Map<String, String> register(AuthRequestDTO data) {
    Map<String, String> response = Map.of("token", "nuevo token obtenido");
    return response;
  }
  
  public Map<String, String> login(AuthRequestDTO data) {
    Map<String, String> response = Map.of("token", "token existente obtenido");
    return response;
  }
}

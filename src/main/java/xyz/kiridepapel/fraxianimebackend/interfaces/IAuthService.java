package xyz.kiridepapel.fraxianimebackend.interfaces;

import xyz.kiridepapel.fraxianimebackend.dtos.AuthRequestDTO;
import xyz.kiridepapel.fraxianimebackend.dtos.ResponseDTO;

public interface IAuthService {
  public ResponseDTO register(AuthRequestDTO data);
  public ResponseDTO login(AuthRequestDTO data);
}

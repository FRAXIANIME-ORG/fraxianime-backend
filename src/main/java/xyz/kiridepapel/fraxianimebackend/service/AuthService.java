package xyz.kiridepapel.fraxianimebackend.service;

// import java.util.NoSuchElementException;

// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.AuthenticationException;
// import org.springframework.security.core.userdetails.UserDetails;
// import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import xyz.kiridepapel.fraxianimebackend.dto.AuthRequestDTO;
import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
// import xyz.kiridepapel.fraxianimebackend.entity.UserEntity;
// import xyz.kiridepapel.fraxianimebackend.entity.enums.RoleEnum;
// import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.InvalidUserOrPassword;
// import xyz.kiridepapel.fraxianimebackend.repository.UserRepository;
// import xyz.kiridepapel.fraxianimebackend.utils.JwtUtils;

@Service
@SuppressWarnings("all")
public class AuthService {
  // @Autowired
  // private UserRepository userRepository;
  // @Autowired
  // private JwtUtils jwtUtils;
  // @Autowired
  // private AuthenticationManager authenticationManager;
  // @Autowired
  // private PasswordEncoder passwordEncoder;

  public ResponseDTO register(AuthRequestDTO data) {
    return null;
  }
  public ResponseDTO login(AuthRequestDTO data) {
    return null;
  }

  // public ResponseDTO register(AuthRequestDTO data) {
  //   // Validar que el usuario no exista
  //   if (userRepository.existsByUsername(data.getUsername())) {
  //     throw new InvalidUserOrPassword("Nombre de usuario no disponible");
  //   }

  //   // Crear al usuario
  //   UserEntity user = UserEntity.builder()
  //       .username(data.getUsername())
  //       .password(passwordEncoder.encode(data.getPassword()))
  //       .role(RoleEnum.USER)
  //       .build();

  //   // Guardar al usuario
  //   userRepository.save(user);

  //   // Retornar un nuevo token
  //   return ResponseDTO.builder()
  //       .token(jwtUtils.genToken(user))
  //       .build();
  // }

  // public ResponseDTO login(AuthRequestDTO data) {
  //   // Buscar al usuario
  //   UserDetails user;
  //   try {
  //     user = userRepository.findByUsername(data.getUsername()).orElseThrow();
  //   } catch (NoSuchElementException e) {
  //     throw new InvalidUserOrPassword("Usuario o contraseña incorrectos");
  //   }
    
  //   // Autenticar al usuario
  //   try {
  //     authenticationManager.authenticate(
  //         new UsernamePasswordAuthenticationToken(data.getUsername(), data.getPassword()));
  //   } catch (AuthenticationException e) {
  //     throw new InvalidUserOrPassword("Usuario o contraseña incorrectos");
  //   }

  //   // Retornar un nuevo token
  //   return ResponseDTO.builder()
  //       .token(jwtUtils.genToken(user))
  //       .build();
  // }

  // public ResponseDTO logout(String realToken) {
  //   // Si el token no esta en la lista negra, agregarlo
  //   if (!jwtUtils.isTokenBlacklisted(realToken)) {
  //     jwtUtils.addTokenToBlacklist(realToken);
  //     return new ResponseDTO("Sesion cerrada con exito", 200);
  //   } else {
  //     return new ResponseDTO("Token no disponible", 400);
  //   }
  // }
}

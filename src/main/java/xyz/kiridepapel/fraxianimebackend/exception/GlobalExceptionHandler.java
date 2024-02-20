package xyz.kiridepapel.fraxianimebackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.*;
import xyz.kiridepapel.fraxianimebackend.exception.DataExceptions.*;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.*;

@ControllerAdvice
public class GlobalExceptionHandler {
  // Security Exceptions
  @ExceptionHandler(InvalidUserOrPassword.class)
  public ResponseEntity<?> handleInvalidUserOrPassword(InvalidUserOrPassword ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }
  
  @ExceptionHandler(ProtectedResource.class)
  public ResponseEntity<?> handleProtectedResource(ProtectedResource ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 401);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }

  @ExceptionHandler(SQLInjectionException.class)
  public ResponseEntity<?> handleSQLInjectionException(SQLInjectionException ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 401);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }

  // Data Exceptions
  @ExceptionHandler(ConnectionFailed.class)
  public ResponseEntity<?> handleConnectionFailed(ConnectionFailed ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 500);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }
  
  @ExceptionHandler(NextTrySearch.class)
  public ResponseEntity<?> handleNextTrySearch(NextTrySearch ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }

  @ExceptionHandler(DataNotFoundException.class)
  public ResponseEntity<?> handleDataNotFoundException(DataNotFoundException ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }
  
  @ExceptionHandler(ArgumentRequiredException.class)
  public ResponseEntity<?> handleIllegalArgumentException(ArgumentRequiredException ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }

  // Anime Exceptions
  @ExceptionHandler(ChapterNotFound.class)
  public ResponseEntity<?> handleChapterNotFound(ChapterNotFound ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }

  @ExceptionHandler(AnimeNotFound.class)
  public ResponseEntity<?> handleAnimeNotFound(AnimeNotFound ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }

  @ExceptionHandler(SearchException.class)
  public ResponseEntity<?> handleSearchException(SearchException ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }

  @ExceptionHandler(InvalidSearch.class)
  public ResponseEntity<?> handleInvalidSearch(InvalidSearch ex) {
    ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }

  @ExceptionHandler(NumberFormatException.class)
  public ResponseEntity<?> handleNumberFormatException(NumberFormatException ex) {
    ResponseDTO response = new ResponseDTO("El capitulo solicitado no existe", 404);
    return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
  }
}

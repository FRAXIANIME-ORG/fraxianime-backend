package xyz.kiridepapel.fraxianimebackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.InvalidSearch;
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.ProtectedResource;
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.SQLInjectionException;

@ControllerAdvice
public class GlobalExceptionHandler {
    // Security Exceptions
    @ExceptionHandler(ProtectedResource.class)
    public ResponseEntity<?> handleProtectedResource(ProtectedResource ex) {
        ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

    @ExceptionHandler(SQLInjectionException.class)
    public ResponseEntity<?> handleSQLInjectionException(SQLInjectionException ex) {
        ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }
    
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<?> handleNumberFormatException(NumberFormatException ex) {
        ResponseDTO response = new ResponseDTO("El capitulo solicitado no existe", 404);
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
    
    @ExceptionHandler(InvalidSearch.class)
    public ResponseEntity<?> handleInvalidSearch(InvalidSearch ex) {
        ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

}

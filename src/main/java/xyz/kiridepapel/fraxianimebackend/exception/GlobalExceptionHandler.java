package xyz.kiridepapel.fraxianimebackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import xyz.kiridepapel.fraxianimebackend.dto.ResponseDTO;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.AnimeNotFound;
import xyz.kiridepapel.fraxianimebackend.exception.AnimeExceptions.ChapterNotFound;
import xyz.kiridepapel.fraxianimebackend.exception.SecurityExceptions.ProtectedResource;

@ControllerAdvice
public class GlobalExceptionHandler {
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
    
    @ExceptionHandler(ProtectedResource.class)
    public ResponseEntity<?> handleProtectedResource(ProtectedResource ex) {
        ResponseDTO response = new ResponseDTO(ex.getMessage(), 404);
        return new ResponseEntity<>(response, HttpStatus.valueOf(response.getStatus()));
    }

}

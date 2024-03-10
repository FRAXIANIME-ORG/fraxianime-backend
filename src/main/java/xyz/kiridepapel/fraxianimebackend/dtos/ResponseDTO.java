package xyz.kiridepapel.fraxianimebackend.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseDTO {
  private String message;
  private Integer code;
  private String token;

  public ResponseDTO(String message, Integer code) {
    this.message = message;
    this.code = code;
  }
}

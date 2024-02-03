package xyz.kiridepapel.fraxianimebackend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDataDTO;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchDTO extends ResponseDTO {
  Integer lastPage;
  List<AnimeDataDTO> searchList;
}

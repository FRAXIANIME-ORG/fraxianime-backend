package xyz.kiridepapel.fraxianimebackend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomePageDTO {
    List<AnimeDTO> lastChapters;
    List<AnimeDTO> allAnimes;
    List<AnimeDTO> emisionAnimes;
}
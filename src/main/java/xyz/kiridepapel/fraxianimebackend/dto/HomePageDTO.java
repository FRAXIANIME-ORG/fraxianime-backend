package xyz.kiridepapel.fraxianimebackend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LastAnimeInfoDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.TopDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomePageDTO {
    List<ChapterDTO> sliderAnimes;
    List<LastAnimeInfoDTO> ovasOnasSpecials;
    List<ChapterDTO> animesProgramming;
    List<ChapterDTO> donghuasProgramming;
    List<TopDTO> topAnimes;
    List<LastAnimeInfoDTO> latestAddedAnimes;
    List<LinkDTO> latestAddedList;
}
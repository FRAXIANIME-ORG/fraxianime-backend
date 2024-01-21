package xyz.kiridepapel.fraxianimebackend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LastAnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.TopDataDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomePageDTO {
    List<ChapterDataDTO> sliderAnimes;
    List<LastAnimeDataDTO> ovasOnasSpecials;
    List<ChapterDataDTO> animesProgramming;
    List<ChapterDataDTO> donghuasProgramming;
    List<TopDataDTO> topAnimes;
    List<LastAnimeDataDTO> latestAddedAnimes;
    List<LinkDTO> latestAddedList;
}
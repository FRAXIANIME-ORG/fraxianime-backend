package xyz.kiridepapel.fraxianimebackend.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LastAnimeDataDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimeInfoDTO {
    private String name;
    private String alternativeName;
    private String imgUrl;
    private String sinopsis;
	private String trailer;

    private Map<String, Object> data;

    private String nextChapterDate;
    private Integer firstChapter;
    private Integer lastChapter;

    private List<LastAnimeDataDTO> recomendations;

}
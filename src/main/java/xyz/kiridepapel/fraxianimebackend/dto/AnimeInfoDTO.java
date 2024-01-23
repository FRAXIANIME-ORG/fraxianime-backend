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
    private String imgUrl;
    private String sinopsis;
	private Integer likes;

    private Map<String, Object> data;
    private Map<String, Object> alternativeTitles;

	private String ytTrailerId;
    private List<LastAnimeDataDTO> recomendations;
	// private Integer lastChapterNumber;
	
    // private List<ChapterDataDTO> chapters;
    // private List<String> genres;

}
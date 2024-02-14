package xyz.kiridepapel.fraxianimebackend.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
// import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LastAnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDataDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimeInfoDTO implements Serializable {
  private String name;
  private String alternativeName;
  private String imgUrl;
  private String synopsis;
  private String synopsisEnglish;
  private String trailer;
  private Integer likes;

  private Map<String, Object> data;
  private Map<String, Object> alternativeTitles;
  private Map<String, Object> history;

  private Integer lastChapter;
  private String lastChapterDate;
  private String nextChapterDate;

  // Alter
  private Boolean isNewestChapter;

  private List<ChapterDataDTO> chapterList;

  // private List<LastAnimeDataDTO> recomendations;
}
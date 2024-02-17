package xyz.kiridepapel.fraxianimebackend.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDataDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.TopDataDTO;

public class PageDTO implements Serializable {
  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class HomePageDTO implements Serializable {
    List<ChapterDataDTO> sliderAnimes;
    List<AnimeDataDTO> ovasOnasSpecials;
    List<ChapterDataDTO> animesProgramming;
    List<ChapterDataDTO> nextAnimesProgramming;
    List<ChapterDataDTO> donghuasProgramming;
    List<TopDataDTO> topAnimes;
    List<AnimeDataDTO> latestAddedAnimes;
    List<LinkDTO> latestAddedList;
  }

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AnimeInfoDTO implements Serializable {
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

    private String lastChapter;
    private String lastChapterDate;
    private String nextChapterDate;

    private List<ChapterDataDTO> chapterList;

    // Alter
    // private Boolean isNewestChapter;
    // private List<LastAnimeDataDTO> recomendations;
  }

  @Builder
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ChapterDTO implements Serializable {
    private String name;
    private List<LinkDTO> srcOptions;
    private List<LinkDTO> downloadOptions;
    private Boolean havePreviousChapter;
    private Boolean haveNextChapter;
    private String nextChapterDate;
    private Boolean inEmision;

    // External
    private String actualChapter;
    private String previousChapter;
    private String nextChapter;
    private String firstChapter;
    private String lastChapter;

    private String chapterImg;
    private String lastChapterDate;
  }

  @Builder
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class SearchDTO {
    Integer lastPage;
    List<AnimeDataDTO> searchList;
    String message; // Cuando no se encuentra nada
  }

}

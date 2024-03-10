package xyz.kiridepapel.fraxianimebackend.dtos;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class IndividualDTO implements Serializable {
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ChapterDataDTO implements Serializable {
    private String name;
    private String imgUrl;
    private String type;
    private String chapter;
    private String date;
    private String time;
    private String url;
    private Boolean state;
  }
  
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class TopDataDTO implements Serializable {
    private String name;
    private String imgUrl;
    private Integer likes;
    private Integer position;
    private String url;
    // Extras for section TOP
    private String type;
    private Integer chapters;
    private String synopsis;
    private String synopsisEnglish;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AnimeDataDTO implements Serializable {
    private String name;
    private String imgUrl;
    private String url;
    private String state;
    private String type;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class LinkDTO implements Serializable {
    private String name;
    private String url;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class AnimeHistoryDTO implements Serializable {
    private String name;
    private String type;
    private String url;
  }

}

package xyz.kiridepapel.fraxianimebackend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.AnimeDTO;

public class PageDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class HomePageDTO {
        List<AnimeDTO> lastChapters;
        List<AnimeDTO> allAnimes;
        List<AnimeDTO> emisionAnimes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AnimeInfoDTO {
        private String name;
        private String sinopsis;
        private String imgUrl;
        private List<AnimeDTO> chapters;
        private List<String> genres;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpecificChapterDTO {
        private String name;
        private String iframeSrc;
        private String previousChapterUrl;
        private String nextChapterUrl;
    }
}

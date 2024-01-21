package xyz.kiridepapel.fraxianimebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class IndividualDTO {
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChapterDataDTO {
        private String name;
        private String imgUrl;
        private String chapter;
        private String date;
        private String url;
        private Boolean state;
        // Available
        // Unavailable
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TopDataDTO {
        private String name;
        private String imgUrl;
        private Integer likes;
        private Integer position;
        private String url;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LastAnimeDataDTO {
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
    public static class LinkDTO {
        private String name;
        private String url;
    }

}

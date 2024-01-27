package xyz.kiridepapel.fraxianimebackend.dto;

import java.io.Serializable;

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
    public static class ChapterDataDTO implements Serializable {
        private String name;
        private String imgUrl;
        private String type;
        private String chapter;
        private String date;
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
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class LastAnimeDataDTO implements Serializable {
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

}

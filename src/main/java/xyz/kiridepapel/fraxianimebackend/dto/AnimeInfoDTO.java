package xyz.kiridepapel.fraxianimebackend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.ChapterDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnimeInfoDTO {
    private String name;
    private String sinopsis;
    private String imgUrl;
    private List<ChapterDTO> chapters;
    private List<String> genres;
}
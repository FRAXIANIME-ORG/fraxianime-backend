package xyz.kiridepapel.fraxianimebackend.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import xyz.kiridepapel.fraxianimebackend.dto.IndividualDTO.LinkDTO;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChapterDTO {
    private String name;
    private String actualChapterNumber;
    private List<LinkDTO> srcOptions;
    private List<LinkDTO> downloadOptions;
    private Boolean havePreviousChapter;
    private Boolean haveNextChapter;
    private String nextChapterDate;
    private String state;

    // External
    private int lastChapterNumber;
    private String lastChapterImg;
    private String lastChapterDate;
}
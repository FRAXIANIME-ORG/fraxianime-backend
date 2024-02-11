package xyz.kiridepapel.fraxianimebackend.dto;

import java.io.Serializable;
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
public class ChapterDTO implements Serializable {
  private String name;
  private List<LinkDTO> srcOptions;
  private List<LinkDTO> downloadOptions;
  private Boolean havePreviousChapter;
  private Boolean haveNextChapter;
  private String nextChapterDate;
  private Boolean inEmision;

  // External
  private String chapterImg;
  private Integer actualChapter;
  private Integer firstChapter;
  private Integer lastChapter;
  private String lastChapterDate;
}
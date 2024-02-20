package xyz.kiridepapel.fraxianimebackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecialCaseDTO {
  private Short type;

  private String jkName;
  private String lfName;
  private String jkUrl;
  private String lfUrl;

  private Boolean changeName;
  private Boolean changeUrlAnime;
  private Boolean changeUrlChapter;
}

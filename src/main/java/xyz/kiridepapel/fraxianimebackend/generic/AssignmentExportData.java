package xyz.kiridepapel.fraxianimebackend.generic;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentExportData<U> {
  private String dataName;
  private Class<?> clazz;
  private List<? extends U> listRetrieved;

  public AssignmentExportData(Class<?> clazz) {
    this.clazz = clazz;
  }

  public AssignmentExportData(Class<?> clazz, List<? extends U> listRetrieved) {
    this.clazz = clazz;
    this.listRetrieved = listRetrieved;
  }
}

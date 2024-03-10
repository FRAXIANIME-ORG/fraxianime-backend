package xyz.kiridepapel.fraxianimebackend.interfaces;

import java.io.InputStream;

import xyz.kiridepapel.fraxianimebackend.dtos.SpecialCaseDTO;

public interface IDataService<T> {
  public byte[] exportExcel(String dataName);
  public void importExcel(String dataName, InputStream inputStream);
  public void newSpecialCase(SpecialCaseDTO data);
  public void updateTop();
}

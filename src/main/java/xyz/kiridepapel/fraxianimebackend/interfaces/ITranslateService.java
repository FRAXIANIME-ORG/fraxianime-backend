package xyz.kiridepapel.fraxianimebackend.interfaces;

public interface ITranslateService {
  public String getTranslatedAndSave(String name, String text, String to);
  public String getTranslated(String name, String from);
}

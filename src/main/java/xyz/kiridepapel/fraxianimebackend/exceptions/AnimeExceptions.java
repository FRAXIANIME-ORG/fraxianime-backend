package xyz.kiridepapel.fraxianimebackend.exceptions;

public class AnimeExceptions {
  public static class ChapterNotFound extends RuntimeException {
    public ChapterNotFound(String message) {
      super(message);
    }
  }

  public static class AnimeNotFound extends RuntimeException {
    public AnimeNotFound(String message) {
      super(message);
    }
  }

  public static class SearchException extends RuntimeException {
    public SearchException(String message) {
      super(message);
    }
  } 

  public static class InvalidSearch extends RuntimeException {
    public InvalidSearch(String message) {
      super(message);
    }
  }
}

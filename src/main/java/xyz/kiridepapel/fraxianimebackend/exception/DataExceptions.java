package xyz.kiridepapel.fraxianimebackend.exception;

public class DataExceptions {
  public static class ConnectionFailed extends RuntimeException {
    public ConnectionFailed(String message) {
      super(message);
    }
  }

  public static class NextTrySearch extends RuntimeException {
    public NextTrySearch() {
      super();
    }
  }

  public static class DataNotFoundException extends RuntimeException {
    public DataNotFoundException(String message) {
      super(message);
    }
  }
  
  public static class ArgumentRequiredException extends RuntimeException {
    public ArgumentRequiredException(String message) {
      super(message);
    }
  }
}

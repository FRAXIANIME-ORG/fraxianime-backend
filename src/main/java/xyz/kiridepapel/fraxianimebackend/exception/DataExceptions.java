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

  public static class DataNotFound extends RuntimeException {
    public DataNotFound(String message) {
      super(message);
    }
  }
}

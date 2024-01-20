package xyz.kiridepapel.fraxianimebackend.exception;

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
    
    public static class RepeatedChapter extends RuntimeException {
        public RepeatedChapter(String message) {
            super(message);
        }
    }

}

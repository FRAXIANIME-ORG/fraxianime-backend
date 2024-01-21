package xyz.kiridepapel.fraxianimebackend.exception;

public class SecurityExceptions {
    public static class ProtectedResource extends RuntimeException {
        public ProtectedResource(String message) {
            super(message);
        }
    }
    
}

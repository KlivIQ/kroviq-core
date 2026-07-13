package kroviq.utils;

public class FatalFrameworkException extends RuntimeException {
    public FatalFrameworkException(String message) {
        super(message);
    }

    public FatalFrameworkException(String message, Throwable cause) {
        super(message, cause);
    }
}

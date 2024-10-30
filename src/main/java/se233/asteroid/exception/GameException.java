package se233.asteroid.exception;

// Base exception for all game-related exceptions
// Exception หลัก
public class GameException extends RuntimeException {
    public GameException(String message) {
        super(message);
    }

    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}

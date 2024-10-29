package se233.asteroid.exception;

public class GameInitializationException extends GameException {
    public GameInitializationException(String message) {
        super(message);
    }

    public GameInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
package se233.asteroid.exception;
//สำหรับ Error สถานะเกม
public class GameStateException extends GameException {
    public GameStateException(String message) {
        super(message);
    }

    public GameStateException(String message, Throwable cause) {
        super(message, cause);
    }
}

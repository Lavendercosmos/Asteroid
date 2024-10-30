package se233.asteroid.exception;
//สำหรับ Error ตอนเริ่มเกม
public class GameInitializationException extends GameException {
    public GameInitializationException(String message) {
        super(message);
    }

    public GameInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
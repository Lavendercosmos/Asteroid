package se233.asteroid.exception;
//สำหรับ Error โหลดทรัพยากร
public class ResourceLoadException extends GameException {
    public ResourceLoadException(String message) {
        super(message);
    }

    public ResourceLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}

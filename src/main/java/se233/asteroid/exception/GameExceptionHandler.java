package se233.asteroid.exception;

import javafx.application.Platform;
import javafx.scene.control.Alert;

//ตัวจัดการ Exception
public class GameExceptionHandler {

    public static void handleException(GameException ex) {
        if (ex instanceof ResourceLoadException) {
            handleResourceLoadException((ResourceLoadException) ex);
        } else if (ex instanceof GameInitializationException) {
            handleInitializationException((GameInitializationException) ex);
        } else if (ex instanceof GameStateException) {
            handleGameStateException((GameStateException) ex);
        } else {
            handleGenericGameException(ex);
        }
    }

    private static void handleResourceLoadException(ResourceLoadException ex) {
        // Show error dialog to user
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Resource Load Error");
            alert.setHeaderText("Failed to Load Game Resource");
            alert.setContentText("The game failed to load a required resource. Please verify your game installation.\n\nError: " + ex.getMessage());
            alert.showAndWait();
        });
    }

    private static void handleInitializationException(GameInitializationException ex) {

        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Initialization Error");
            alert.setHeaderText("Game Initialization Failed");
            alert.setContentText("The game failed to initialize properly.\n\nError: " + ex.getMessage());
            alert.showAndWait();
        });
    }

    private static void handleGameStateException(GameStateException ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Game State Error");
            alert.setHeaderText("Game State Error Occurred");
            alert.setContentText("An error occurred during gameplay.\n\nError: " + ex.getMessage());
            alert.showAndWait();
        });
    }

    private static void handleGenericGameException(GameException ex) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Game Error");
            alert.setHeaderText("Unexpected Error");
            alert.setContentText("An unexpected error occurred.\n\nError: " + ex.getMessage());
            alert.showAndWait();
        });
    }
}
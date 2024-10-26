package se233.asteroid;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import se233.asteroid.display.GameStage;

public class Launcher extends Application {
    @Override
    public void start(Stage stage) {
        GameStage gameStage = new GameStage();
        Scene scene = new Scene(gameStage, 800, 600);

        stage.setTitle("Asteroid Game");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
package se233.asteroid.View;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

public class GameStage extends Pane {
    private ImageView backgroundView;

    public GameStage() {
        // Load the GIF using JavaFX Image
        Image gifImage = new Image(getClass().getResourceAsStream("/se233/asteroid/assets/Backgrounds/SpaceBG.gif"));
        backgroundView = new ImageView(gifImage);

        // Make background image resize with the window
        backgroundView.fitWidthProperty().bind(this.widthProperty());
        backgroundView.fitHeightProperty().bind(this.heightProperty());

        // Add background to the pane
        getChildren().add(backgroundView);
    }
}


module se233.asteroid {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens se233.asteroid to javafx.fxml;
    exports se233.asteroid;
}
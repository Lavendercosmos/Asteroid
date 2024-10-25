module se233.asteroid {
    requires javafx.controls;
    requires javafx.fxml;


    opens se233.asteroid to javafx.fxml;
    exports se233.asteroid;
}
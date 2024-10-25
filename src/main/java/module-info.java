module se.asteroid {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires org.apache.logging.log4j;

    opens se233.asteroid to javafx.fxml;
    exports se233.asteroid;
}
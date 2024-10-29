module se233.asteroid {
    // Required JavaFX modules
    requires javafx.controls;
    requires javafx.graphics;
    requires org.apache.logging.log4j;

    // Export the package containing your main application class
    exports se233.asteroid;
    exports se233.asteroid.view;
    exports se233.asteroid.model;

}
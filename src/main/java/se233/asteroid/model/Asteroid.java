package se233.asteroid.model;

import javafx.geometry.Point2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Asteroid extends Character {
    private static final Logger logger = LogManager.getLogger(Asteroid.class);
    private final int size; // 1 for small, 2 for large
    private final int points;

    public Asteroid(Point2D position, int size) {
        super(size == 1 ? "/se233/asteroid/assets/Sprites/small_asteroid.png"
                        : "/se233/asteroid/assets/Sprites/large_asteroid.png",
                position,
                size == 1 ? 15 : 30);
        this.size = size;
        this.points = size;

        // Random velocity
        double speed = size == 1 ? 3.0 : 2.0;
        double angle = Math.random() * 2 * Math.PI;
        this.velocity = new Point2D(Math.cos(angle) * speed, Math.sin(angle) * speed);

        logger.info("Created {} asteroid at position: {}", size == 1 ? "small" : "large", position);
    }

    @Override
    public void update() {
        super.update();
        rotation += (size == 1 ? 2.0 : 1.0); // Rotate smaller asteroids faster
    }

    public int getPoints() { return points; }
    public int getSize() { return size; }
}
package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.SubScene;
import javafx.scene.shape.Shape;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Bullet extends Character {
    private static final Logger logger = LogManager.getLogger(Bullet.class);
    private static final double BULLET_SPEED = 10.0;
    Shape shape;

    public Bullet(Point2D position, Point2D direction) {
        super("/se233/asteroid/assets/Sprites/bullet.png", position, 5);
        this.velocity = direction.normalize().multiply(BULLET_SPEED);
        logger.debug("Bullet created at position: {} with velocity: {}", position, velocity);
    }

    @Override
    public void update() {
        super.update();
        // Additional bullet-specific update logic could go here
    }

    public Shape getShape() {
        return shape;
    }
}
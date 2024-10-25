package se233.asteroid.model;

import javafx.geometry.Point2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlayerShip extends Character {
    private static final Logger logger = LogManager.getLogger(PlayerShip.class);
    private static final double ROTATION_SPEED = 5.0;
    private static final double MOVEMENT_SPEED = 5.0;
    private static final double SHIP_SIZE = 20.0;

    private int lives;
    private boolean invulnerable;
    private long invulnerabilityTimer;

    public PlayerShip(Point2D position) {
        super("/se233/asteroid/assets/Sprites/player_ship.png", position, SHIP_SIZE);
        this.lives = 3;
        this.invulnerable = false;
        logger.info("PlayerShip initialized at position: {}", position);
    }

    @Override
    public void update() {
        super.update();
        // Apply drag to gradually slow down
        velocity = velocity.multiply(0.98);

        // Update invulnerability
        if (invulnerable && System.currentTimeMillis() - invulnerabilityTimer > 2000) {
            invulnerable = false;
        }
    }

    public void hit() {
        if (!invulnerable) {
            lives--;
            invulnerable = true;
            invulnerabilityTimer = System.currentTimeMillis();
            if (lives <= 0) {
                isAlive = false;
                logger.info("Player ship destroyed");
            }
            logger.debug("Player hit. Lives remaining: {}", lives);
        }
    }

    public void moveUp() {
        double radians = Math.toRadians(rotation - 90);
        velocity = velocity.add(new Point2D(
                Math.cos(radians) * MOVEMENT_SPEED,
                Math.sin(radians) * MOVEMENT_SPEED
        ));
    }

    public void moveDown() {
        double radians = Math.toRadians(rotation - 90);
        velocity = velocity.add(new Point2D(
                -Math.cos(radians) * MOVEMENT_SPEED,
                -Math.sin(radians) * MOVEMENT_SPEED
        ));
    }

    public void moveLeft() {
        double radians = Math.toRadians(rotation);
        velocity = velocity.add(new Point2D(
                -Math.cos(radians) * MOVEMENT_SPEED,
                -Math.sin(radians) * MOVEMENT_SPEED
        ));
    }

    public void moveRight() {
        double radians = Math.toRadians(rotation);
        velocity = velocity.add(new Point2D(
                Math.cos(radians) * MOVEMENT_SPEED,
                Math.sin(radians) * MOVEMENT_SPEED
        ));
    }

    public void rotateLeft() {
        rotation -= ROTATION_SPEED;
        updateSpriteRotation();
    }

    public void rotateRight() {
        rotation += ROTATION_SPEED;
        updateSpriteRotation();
    }

    public Bullet shoot() {
        double radians = Math.toRadians(rotation - 90);
        Point2D bulletVelocity = new Point2D(
                Math.cos(radians) * 10,
                Math.sin(radians) * 10
        );
        Point2D bulletPosition = position.add(
                bulletVelocity.normalize().multiply(SHIP_SIZE)
        );
        return new Bullet(bulletPosition, bulletVelocity);
    }

    public int getLives() {
        return lives;
    }

    public boolean isInvulnerable() {
        return invulnerable;
    }
}
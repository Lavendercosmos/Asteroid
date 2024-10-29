package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Character {
    private static final Logger logger = LogManager.getLogger(Character.class);

    // Screen boundaries
    protected static final double SCREEN_WIDTH = 800.0;
    protected static final double SCREEN_HEIGHT = 600.0;

    // Movement constants
    protected static final double MOVEMENT_SPEED = 5.0;
    protected static final double ROTATION_SPEED = 5.0;
    protected static final double FRICTION = 0.98;
    protected static final double MAX_SPEED = 10.0;

    // Core properties
    protected Point2D position;
    protected Point2D velocity;
    protected double rotation;
    protected ImageView sprite;
    protected double hitRadius;
    protected boolean isAlive;
    protected boolean debugMode;

    public Character(String spritePath, Point2D position, double hitRadius) {
        try {
            // Initialize sprite
            Image image = new Image(getClass().getResourceAsStream(spritePath));
            this.sprite = new ImageView(image);
            this.sprite.setPreserveRatio(true);

            // Initialize properties
            this.position = position;
            this.velocity = new Point2D(0, 0);
            this.rotation = 0;
            this.hitRadius = hitRadius;
            this.isAlive = true;
            this.debugMode = false;

            // Set initial sprite position
            updateSpritePosition();
            updateSpriteRotation();

        } catch (Exception e) {
            logger.error("Failed to load sprite: " + spritePath, e);
            throw new RuntimeException("Failed to initialize character sprite: " + spritePath, e);
        }
    }

    public void update() {
        if (!isAlive) return;

        // Apply friction to velocity
        velocity = velocity.multiply(FRICTION);

        // Limit maximum speed
        if (velocity.magnitude() > MAX_SPEED) {
            velocity = velocity.normalize().multiply(MAX_SPEED);
        }

        // Update position with velocity
        position = position.add(velocity);

        // Handle screen wrapping
        wrapPosition();

        // Update visual elements
        updateSpritePosition();
        updateSpriteRotation();
    }

    protected void wrapPosition() {
        double newX = position.getX();
        double newY = position.getY();

        // Wrap horizontally
        if (newX < 0) newX = SCREEN_WIDTH;
        else if (newX > SCREEN_WIDTH) newX = 0;

        // Wrap vertically
        if (newY < 0) newY = SCREEN_HEIGHT;
        else if (newY > SCREEN_HEIGHT) newY = 0;

        // Only update if position changed
        if (newX != position.getX() || newY != position.getY()) {
            position = new Point2D(newX, newY);
        }
    }

    public boolean collidesWith(Character other) {
        if (!isAlive || !other.isAlive) return false;

        double distance = position.distance(other.getPosition());
        boolean collision = distance < (this.hitRadius + other.hitRadius);

        if (collision && debugMode) {
            logger.debug("Collision detected between characters at {} and {}", position, other.getPosition());
        }

        return collision;
    }

    /**
     * Updates the sprite's position to match the character's position.
     */
    protected void updateSpritePosition() {
        if (sprite != null) {
            double width = sprite.getBoundsInLocal().getWidth();
            double height = sprite.getBoundsInLocal().getHeight();
            sprite.setTranslateX(position.getX() - width/2);
            sprite.setTranslateY(position.getY() - height/2);
        }
    }

    protected void updateSpriteRotation() {
        if (sprite != null) {
            sprite.setRotate(rotation);
        }
    }

    protected void setImage(Image image) {
        if (sprite != null && image != null) {
            sprite.setImage(image);
        } else {
            logger.warn("Attempted to set null image or sprite is null");
        }
    }

    // Movement methods
    public void moveLeft() {
        velocity = velocity.add(new Point2D(-MOVEMENT_SPEED, 0));
    }

    public void moveRight() {
        velocity = velocity.add(new Point2D(MOVEMENT_SPEED, 0));
    }

    public void moveUp() {
        velocity = velocity.add(new Point2D(0, -MOVEMENT_SPEED));
    }

    public void moveDown() {
        velocity = velocity.add(new Point2D(0, MOVEMENT_SPEED));
    }

    public void rotateLeft() {
        rotation = (rotation - ROTATION_SPEED + 360) % 360;
        updateSpriteRotation();
    }

    public void rotateRight() {
        rotation = (rotation + ROTATION_SPEED) % 360;
        updateSpriteRotation();
    }

    // Getters and setters
    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        this.position = position;
        updateSpritePosition();
    }

    public Point2D getVelocity() {
        return velocity;
    }

    public void setVelocity(Point2D velocity) {
        this.velocity = velocity;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
        updateSpriteRotation();
    }

    public ImageView getSprite() {
        return sprite;
    }

    public double getHitRadius() {
        return hitRadius;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }
}
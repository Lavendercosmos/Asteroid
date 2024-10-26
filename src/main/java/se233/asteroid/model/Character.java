package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.Group;

public abstract class Character {
    private static final Logger logger = LogManager.getLogger(Character.class);

    protected Point2D position;
    protected Point2D velocity;
    protected double rotation;
    protected ImageView sprite;
    protected double hitRadius;
    protected boolean isAlive;
    protected boolean debugMode = false;

    public Character(String spritePath, Point2D position, double hitRadius) {
        try {
            Image image = new Image(getClass().getResourceAsStream(spritePath));
            this.sprite = new ImageView(image);
            this.position = position;
            this.velocity = new Point2D(0, 0);
            this.rotation = 0;
            this.hitRadius = hitRadius;
            this.isAlive = true;

            // Center the sprite on position
            sprite.setTranslateX(position.getX() - image.getWidth()/2);
            sprite.setTranslateY(position.getY() - image.getHeight()/2);

            logger.debug("Character created with sprite: {}", spritePath);
        } catch (Exception e) {
            logger.error("Failed to load sprite: " + spritePath, e);
            throw new RuntimeException("Failed to load sprite: " + spritePath, e);
        }
    }

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

    public boolean isAlive() {
        return isAlive;
    }

    public void update() {
        position = position.add(velocity);
        updateSpritePosition();
    }

    public boolean collidesWith(Character other) {
        if (!isAlive || !other.isAlive) return false;

        double distance = position.distance(other.getPosition());
        return distance < (this.hitRadius + other.hitRadius);
    }

    protected void updateSpritePosition() {
        if (sprite != null) {
            sprite.setTranslateX(position.getX() - sprite.getImage().getWidth()/2);
            sprite.setTranslateY(position.getY() - sprite.getImage().getHeight()/2);
        }
    }

    protected void updateSpriteRotation() {
        if (sprite != null) {
            sprite.setRotate(rotation);
        }

    }
}
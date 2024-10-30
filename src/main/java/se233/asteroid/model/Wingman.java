package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import java.util.ArrayList;
import java.util.List;

public class Wingman extends Character {
    private static final double SHOOT_COOLDOWN = 0.5; // 0.5 seconds cooldown
    private static final double UNLOCK_SCORE_FIRST = 5; // Score needed for first wingman
    private static final double UNLOCK_SCORE_SECOND = 10; // Score needed for second wingman
    private static final double FORMATION_OFFSET = 30.0; // Distance from main ship
    private static final double ROTATION_OFFSET = 45.0; // Degrees offset from main ship
    private static final String WINGMAN_SPRITE_PATH = "/se233/asteroid/assets/Wingman/wingman.png"; // Assuming this path exists
    private static final double WINGMAN_SIZE = 15.0;

    private double lastShootTime = 0;
    private int position; // 1 for left wing, 2 for right wing
    private PlayerShip leader;
    private double health;
    private boolean isActive;
    private Point2D velocity;

    public Wingman(PlayerShip leader, int position) {
        super(WINGMAN_SPRITE_PATH, new Point2D(0, 0), WINGMAN_SIZE);
        this.leader = leader;
        this.position = position;
        this.health = 50;
        this.isActive = true;
        this.velocity = new Point2D(0, 0);
        updatePosition();
    }

    public void setActive(boolean active) {
        this.isActive = active;
        sprite.setVisible(active); // Update sprite visibility
        if (!active) {
            // Additional cleanup when deactivating
            health = 0;
        }
    }

    @Override
    public void update() {
        if (isActive) {
            updatePosition();
        }
    }

    private void updatePosition() {
        if (leader != null) {
            // Calculate offset based on position (left or right wing)
            double angleOffset = position == 1 ? -ROTATION_OFFSET : ROTATION_OFFSET;
            double angle = Math.toRadians(leader.getRotation() + angleOffset);

            // Calculate new position relative to leader
            double offsetX = FORMATION_OFFSET * Math.sin(angle);
            double offsetY = FORMATION_OFFSET * Math.cos(angle);

            Point2D leaderPos = leader.getPosition();
            setPosition(new Point2D(
                    leaderPos.getX() + offsetX,
                    leaderPos.getY() - offsetY
            ));

            // Match leader's rotation
            sprite.setRotate(leader.getRotation());
        }
    }

    public Bullet shoot() {
        if (!isActive) return null;

        double currentTime = System.nanoTime() / 1_000_000_000.0; // Convert to seconds
        if (currentTime - lastShootTime < SHOOT_COOLDOWN) {
            return null;
        }

        lastShootTime = currentTime;

        // Calculate direction based on rotation
        double angle = Math.toRadians(sprite.getRotate() - 90); // -90 because sprite points upward by default
        Point2D direction = new Point2D(Math.cos(angle), Math.sin(angle));

        // Create bullet
        Bullet bullet = new Bullet(getPosition(), direction, false) {
            @Override
            public int getDamage() {
                return 15; // Higher damage than regular bullets
            }
        };

        return bullet;
    }

    public void hit() {
        health -= 25;
        if (health <= 0) {
            setActive(false); // Use setActive instead of direct assignment
        }
    }

    public static boolean canUnlockWingman(int currentWingmen, int score) {
        if (currentWingmen == 0 && score >= UNLOCK_SCORE_FIRST) {
            return true;
        }
        return currentWingmen == 1 && score >= UNLOCK_SCORE_SECOND;
    }


    public void reset() {
        health = 50;
        isActive = true;
        lastShootTime = 0;
        sprite.setVisible(true);
    }



    // Getter methods
    public static double getUnlockScoreFirst() { return UNLOCK_SCORE_FIRST; }
    public static double getUnlockScoreSecond() { return UNLOCK_SCORE_SECOND; }
    public double getHealth() { return health; }
    public boolean isActive() {return isActive;}
}

package se233.asteroid.model;

import javafx.geometry.Point2D;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Boss extends Character {
    private static final Logger logger = LogManager.getLogger(Boss.class);
    private static final int POINTS = 10;
    private int attackPattern = 0;
    private double timeSinceLastAttack = 0;
    private int health ;
    public Boss(Point2D position) {
        super("/se233/asteroid/assets/Sprites/boss.png", position, 50);
        health = 200;
        logger.info("Boss created at position: {} with health: {}", position, health);
    }

    public void update(double deltaTime, Point2D playerPosition) {
        timeSinceLastAttack += deltaTime;

        // Change attack pattern every 5 seconds
        if (timeSinceLastAttack >= 5.0) {
            attackPattern = (attackPattern + 1) % 3;
            timeSinceLastAttack = 0;
            logger.info("Boss changing to attack pattern: {}", attackPattern);
        }

        // Update movement based on attack pattern
        switch (attackPattern) {
            case 0: // Circle pattern
                circlePattern(deltaTime);
                break;
            case 1: // Chase player
                chasePattern(playerPosition);
                break;
            case 2: // Zigzag pattern
                zigzagPattern(deltaTime);
                break;
        }

        super.update();
    }

    private void circlePattern(double deltaTime) {
        double radius = 100;
        double speed = 2.0;
        double angle = timeSinceLastAttack * speed;
        position = new Point2D(
                400 + Math.cos(angle) * radius,
                300 + Math.sin(angle) * radius
        );
    }

    private void chasePattern(Point2D playerPosition) {
        Point2D direction = playerPosition.subtract(position).normalize();
        velocity = direction.multiply(3.0);
    }

    private void zigzagPattern(double deltaTime) {
        double amplitude = 100;
        double frequency = 2.0;
        velocity = new Point2D(
                3.0,
                amplitude * Math.sin(timeSinceLastAttack * frequency)
        );
    }

    public Bullet[] shootSpecialAttack() {
        Bullet[] bullets = new Bullet[8];
        for (int i = 0; i < 8; i++) {
            double angle = i * 45;
            double radians = Math.toRadians(angle);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            bullets[i] = new Bullet(position, direction);
        }
        logger.info("Boss firing special attack");
        return bullets;
    }

    public void hit(int damage) {
        health -= damage;
        logger.info("Boss hit! Health remaining: {}", health);
        if (health <= 0) {
            isAlive = false;
            logger.info("Boss defeated!");
        }
    }

    public int getPoints() { return POINTS; }
    public double getHealth() { return health; }
}
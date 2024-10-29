package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.ImageView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpecialAttack extends Character {
    private static final Logger logger = LogManager.getLogger(Character.class);

    // ปรับ path ให้ชี้ไปที่ sprite sheet ของ missile
    private static final String MISSILE_SPRITE_PATH = "/se233/asteroid/assets/PlayerShip/missile.png";
    private static final double MISSILE_SIZE = 25.0; // ขนาดใหญ่กว่ากระสุนปกติ
    private static final double MISSILE_LIFETIME = 3.0; // อายุการใช้งานสั้นกว่า beam
    private static final int MISSILE_DAMAGE = 30; // ความเสียหายมากกว่ากระสุนปกติ
    private static final double MISSILE_COOLDOWN = 10.0; // เวลาคูลดาวน์เท่าเดิม
    private static final double MISSILE_SPEED = 1.0; // ความเร็วช้ากว่ากระสุนปกตินิดหน่อย
    private static final double MISSILE_MAX = 10;

    private boolean active;
    private double lifetime;
    private boolean isEnemyBullet;
    private double acceleration = 0.2; // เพิ่มความเร่งให้ missile
    private double maxSpeed = 15.0; // ความเร็วสูงสุด


    public SpecialAttack(Point2D position, Point2D direction, boolean isEnemyBullet) {
        super(MISSILE_SPRITE_PATH, position, MISSILE_SIZE);
        initializeMissile(direction, isEnemyBullet);
    }

    private void initializeMissile(Point2D direction, boolean isEnemyBullet) {
        try {
            this.active = true;
            this.lifetime = MISSILE_LIFETIME;
            this.isEnemyBullet = isEnemyBullet;
            this.velocity = direction.normalize().multiply(MISSILE_SPEED);

            // ตั้งค่า sprite
            configureSprite();

            logger.debug("Missile initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize missile", e);
            throw new RuntimeException("Failed to initialize missile", e);
        }
    }


    private void configureSprite() {
        try {
            // Ensure sprite is properly loaded
            if (sprite == null) {
                throw new RuntimeException("Sprite not initialized");
            }

            // ตั้งค่าขนาดของ missile
            sprite.setFitWidth(MISSILE_SIZE * 2);
            sprite.setFitHeight(MISSILE_SIZE * 2);
            sprite.setPreserveRatio(true);
            sprite.setVisible(true); // Explicitly set visibility

            // เพิ่ม effect สำหรับ missile
            if (isEnemyBullet) {
                sprite.setStyle("-fx-effect: dropshadow(gaussian, red, 15, 0.7, 0, 0);");
            } else {
                sprite.setStyle("-fx-effect: dropshadow(gaussian, blue, 15, 0.7, 0, 0);");
            }

            // ตั้งค่าการหมุนตามทิศทางการเคลื่อนที่
            double angle = Math.toDegrees(Math.atan2(velocity.getY(), velocity.getX()));
            sprite.setRotate(angle + 90);
            // Debug logging
            logger.debug("Missile sprite configured: size={}, position={}, rotation={}",
                    MISSILE_SIZE, position, angle);
        } catch (Exception e) {
            logger.error("Failed to configure missile sprite", e);
            throw new RuntimeException("Failed to configure missile sprite", e);
        }

    }


    @Override
    public void update() {
        if (active) {
            // เพิ่มความเร็วตามเวลา
            if (velocity.magnitude() < maxSpeed) {
                velocity = velocity.add(velocity.normalize().multiply(acceleration));
            }

            // อัพเดตตำแหน่ง
            super.update();

            // อัพเดตเวลาที่เหลือ
            lifetime -= 1.0 / 60.0;
            if (lifetime <= 0) {
                deactivate();
            }

            // อัพเดตการหมุนของ sprite
            updateSpriteRotation();

            // เพิ่ม particle effect หรือ trail ได้ตรงนี้
            createTrailEffect();
        }
    }

    private void createTrailEffect() {
        // TODO: เพิ่ม particle effect หรือ trail effect
        // สามารถสร้าง particle system หรือใช้ JavaFX effect เพื่อสร้าง trail
    }

    @Override
    protected void updateSpriteRotation() {
        if (velocity.magnitude() > 0) {
            double angle = Math.toDegrees(Math.atan2(velocity.getY(), velocity.getX()));
            sprite.setRotate(angle + 90);
        }
    }

    public void deactivate() {
        active = false;
        sprite.setVisible(false);
        logger.debug("Missile deactivated at position: {}", position);
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public ImageView getSprite() {
        return sprite;
    }

    public boolean isEnemyBullet() {
        return isEnemyBullet;
    }

    @Override
    public String toString() {
        return String.format("Missile [position=(%f,%f), velocity=(%f,%f), active=%b, isEnemy=%b]",
                position.getX(), position.getY(),
                velocity.getX(), velocity.getY(),
                active, isEnemyBullet);
    }
    public boolean isExpired() {
        return lifetime <= 0;
    }
}
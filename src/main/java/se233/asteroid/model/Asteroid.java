package se233.asteroid.model;

import javafx.geometry.Point2D;
import javafx.scene.image.Image;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.SnapshotParameters;
import javafx.scene.paint.Color;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.ArrayList;
import java.util.List;

public class Asteroid extends Character {
    private static final Logger logger = LogManager.getLogger(Asteroid.class);

    // Asset paths
    private static final String ASTEROID_BASE_PATH = "/se233/asteroid/assets/Astroides/Asteroid_Base.png";
    private static final String ASTEROID_EXPLODE_PATH = "/se233/asteroid/assets/Astroides/Asteroid_Explode.png";

    // Constants - simplified for small asteroids only
    private static final double ASTEROID_SPEED = 1.0;
    private static final double ROTATION_SPEED = 2.0;
    private static final double ASTEROID_RADIUS = 15;
    private static final int EXPLOSION_FRAMES = 5;
    private static final Duration FRAME_DURATION = Duration.millis(50); //ในการลดระยะเวลา cooldown ของการระเบิดอุกาบาต

    // Asteroid properties
    private final int points;
    private int size;
    private boolean isExploding;
    private List<Image> explosionFrames;
    private int currentExplosionFrame;
    private Timeline explosionAnimation;
    private double currentSpeed;
    private double directionAngle;
    private boolean isInvulnerable;
    private long invulnerableStartTime;
    private static final long INVULNERABLE_DURATION = 500;
    private double baseWidth;
    private double baseHeight;

    public Asteroid(Point2D position) {
        super(ASTEROID_BASE_PATH, position, ASTEROID_RADIUS);
        this.size = size; // 1 for small, 2 for large
        this.isInvulnerable = false;
        this.points = 100; // Fixed points for destroying asteroid
        this.isExploding = false;
        this.currentSpeed = ASTEROID_SPEED;

        // Set random direction angle in radians
        this.directionAngle = Math.random() * 2 * Math.PI;

        // Store original asteroid dimensions
        this.baseWidth = sprite.getFitWidth();
        this.baseHeight = sprite.getFitHeight();

        // Initialize explosion frames
        this.explosionFrames = loadExplosionFrames();
        setupExplosionAnimation();

        // Set random initial rotation
        this.rotation = Math.random() * 360;

        // Initialize velocity for continuous movement
        initializeVelocity();

        logger.info("Created asteroid at position: {} with speed: {} and angle: {}°",
                position, currentSpeed, Math.toDegrees(directionAngle));
    }

    private void initializeVelocity() {
        // Calculate velocity components based on direction and speed
        double vx = Math.cos(directionAngle) * currentSpeed;
        double vy = Math.sin(directionAngle) * currentSpeed;
        this.velocity = new Point2D(vx, vy);
    }

    public void setInvulnerable(boolean invulnerable) {
        this.isInvulnerable = invulnerable;
        if (invulnerable) {
            this.invulnerableStartTime = System.currentTimeMillis();
        }
    }

    public boolean isInvulnerable() {
        return isInvulnerable;
    }

    @Override
    public void update() {
        // Check invulnerability timeout
        if (isInvulnerable && System.currentTimeMillis() - invulnerableStartTime >= INVULNERABLE_DURATION) {
            isInvulnerable = false;
        }

        if (!isExploding) {
            // Update position based on current velocity
            position = position.add(velocity);

            // Update rotation
            rotation += ROTATION_SPEED;
            if (rotation >= 360) {
                rotation -= 360;
            }

            // Update sprite position and rotation
            updateSpritePosition();
            sprite.setRotate(rotation);

            logger.trace("Asteroid updated - Position: {}, Velocity: {}, Rotation: {}",
                    position, velocity, rotation);
        }
    }

    public void hit() {
        if (isAlive && !isExploding) {
            logger.debug("Asteroid hit at position: {}", position);
            explode();
        }
    }

    public List<Asteroid> split() {
        List<Asteroid> fragments = new ArrayList<>();
        double[] angles = {-45, 45}; // กำหนดทิศทางการกระจาย
        double spreadDistance = 30.0; // ระยะห่างจากจุดเดิม

        for (double angle : angles) {
            // คำนวณตำแหน่งใหม่ให้ห่างจากจุดเดิม
            double radians = Math.toRadians(angle);
            Point2D offset = new Point2D(
                    Math.cos(radians) * spreadDistance,
                    Math.sin(radians) * spreadDistance
            );
            Point2D newPos = position.add(offset);

            // สร้าง fragment ในตำแหน่งใหม่
            Asteroid fragment = new Asteroid(newPos);
            fragments.add(fragment);
        }

        logger.info("Asteroid split into {} fragments", fragments.size());
        return fragments;
    }

    private void explode() {
        if (!isExploding) {
            isExploding = true;
            currentExplosionFrame = 0;

            // ใช้ตำแหน่งปัจจุบันของ asteroid
            double asteroidX = position.getX() - baseWidth/2;
            double asteroidY = position.getY() - baseHeight/2;

            // ไม่ต้องปรับขนาด sprite ให้ใหญ่ขึ้น ใช้ขนาดเดิม
            sprite.setFitWidth(baseWidth );  // ใช้ขนาดเดิมของ asteroid
            sprite.setFitHeight(baseHeight );

            // ปรับตำแหน่งให้ centered
            sprite.setTranslateX(asteroidX);
            sprite.setTranslateY(asteroidY);

            // Start explosion animation
            setupExplosionAnimation();
            explosionAnimation.play();

            // ลบ sprite เมื่อ animation จบ
            explosionAnimation.setOnFinished(event -> {
                sprite.setVisible(false);  // ซ่อน sprite
                sprite.setImage(null);     // ลบรูปภาพ
                isAlive = false;           // ตั้งค่าว่าถูกทำลายแล้ว
            });
        }
    }


    @Override
    protected void updateSpritePosition() {
        if (!isExploding) {
            sprite.setTranslateX(position.getX() - sprite.getFitWidth() / 2);
            sprite.setTranslateY(position.getY() - sprite.getFitHeight() / 2);
        }
    }

    private List<Image> loadExplosionFrames() {
        List<Image> frames = new ArrayList<>();
        try {
            Image explodeSheet = new Image(getClass().getResourceAsStream(ASTEROID_EXPLODE_PATH));

            double frameWidth = explodeSheet.getWidth() / EXPLOSION_FRAMES;
            double frameHeight = explodeSheet.getHeight();

            // คำนวณ scale factor เพื่อให้ explosion มีขนาดเท่ากับ asteroid
            double scaleX = baseWidth / frameWidth;
            double scaleY = baseHeight / frameHeight;
            double scale = Math.min(scaleX, scaleY);

            for (int i = 0; i < EXPLOSION_FRAMES; i++) {
                Canvas canvas = new Canvas(frameWidth* scale, frameHeight* scale);
                GraphicsContext gc = canvas.getGraphicsContext2D();

                gc.drawImage(explodeSheet,
                        i * frameWidth, 0,
                        frameWidth, frameHeight,
                        0, 0,
                        frameWidth * scale, frameHeight * scale);

                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                Image frame = canvas.snapshot(params, null);
                frames.add(frame);
            }

        } catch (Exception e) {
            logger.error("Failed to load explosion frames", e);
        }
        return frames;
    }

    // การระเบิดที่ทำให้ sprite แตก
    private void setupExplosionAnimation() {
        explosionAnimation = new Timeline();

                Duration frameTime = Duration.millis(100);

        for (int i = 0; i < explosionFrames.size(); i++) {
            final int frameIndex = i;
            KeyFrame keyFrame = new KeyFrame(
                    frameTime.multiply(i + 1),
                    e -> {
                        sprite.setImage(explosionFrames.get(frameIndex));
                        sprite.setFitWidth(baseWidth);
                        sprite.setFitHeight(baseHeight);
                    }
            );
            explosionAnimation.getKeyFrames().add(keyFrame);
        }

        explosionAnimation.setOnFinished(e -> {
            sprite.setVisible(false);
            sprite.setImage(null);
            isAlive = false;
        });
    }

    @Override
    public void setPosition(Point2D newPosition) {
        this.position = newPosition;
        if (!isExploding) {
            updateSpritePosition();
        }
    }

    // Getters
    public int getPoints() { return points; }
    public boolean isExploding() { return isExploding; }
    public double getRadius() { return ASTEROID_RADIUS; }
    public int getSize() {return size;}

    // Resource cleanup
    public void dispose() {
        if (explosionAnimation != null) {
            explosionAnimation.stop();
        }
    }
    @Override
    public boolean collidesWith(Character other) {
        // ตรวจสอบเงื่อนไขทั้งหมดที่ไม่ควรเกิดการชน
        if (!isAlive || // ถ้าอุกาบาตถูกทำลายแล้ว
                isExploding || // กำลังระเบิด
                isInvulnerable || // อยู่ในสถานะไม่สามารถชนได้
                !sprite.isVisible() || // sprite ไม่แสดงผล
                other == null || // ไม่มีวัตถุที่จะชน
                !other.isAlive()) { // วัตถุที่จะชนถูกทำลายแล้ว
            return false;
        }

        // คำนวณระยะห่างระหว่างศูนย์กลางของวัตถุทั้งสอง
        double distance = position.distance(other.getPosition());
        double minDistance = getHitRadius() + other.getHitRadius(); // ใช้ hitRadius จาก Character class

        // จะชนกันก็ต่อเมื่อระยะห่างน้อยกว่าผลรวมของรัศมีทั้งสอง
        return distance < minDistance;
    }
}
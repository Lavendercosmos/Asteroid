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
    private static final String METEOR_BASE_PATH = "/se233/asteroid/assets/Astroides/meteor.png";

    // Constants - simplified for small asteroids only
    private static final double ASTEROID_SPEED = 1.0;
    private static final double METEOR_SPEED = 1.0;
    private static final double ROTATION_SPEED = 2.0;
    private static final double ASTEROID_RADIUS = 30;
    private static final double METEOR_RADIUS = 25;
    private static final int EXPLOSION_FRAMES = 5;
    private static final Duration FRAME_DURATION = Duration.millis(100);
    private static final double EXPLOSION_SCALE = 1.5; // Scale factor for explosion

    // Type enumeration
    public enum Type {
        ASTEROID,
        METEOR
    }
    // Asteroid properties
    private final Type type;
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
    private Color trailColor;  // For meteor trail effect

    public Asteroid(Point2D position, Type type) {
        super(type == Type.ASTEROID ? ASTEROID_BASE_PATH : METEOR_BASE_PATH,
                position,
                type == Type.ASTEROID ? ASTEROID_RADIUS : METEOR_RADIUS);

        this.type = type;
        this.isInvulnerable = false;
        this.points = 100; // Fixed points for destroying asteroid
        this.isExploding = false;
        this.currentSpeed = type == Type.ASTEROID ? ASTEROID_SPEED : METEOR_SPEED;

        // Set random direction angle in radians
        this.directionAngle = Math.random() * 2 * Math.PI;

        // Store original asteroid dimensions
        this.baseWidth = sprite.getFitWidth();
        this.baseHeight = sprite.getFitHeight();

        // Set trail color for meteors
        this.trailColor = type == Type.METEOR ?
                Color.rgb(255, 100, 0, 0.6) :  // Orange for meteors
                Color.TRANSPARENT;              // No trail for asteroids

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
            if (rotation >= 360) rotation -= 360;


            // Update sprite position and rotation
            updateSpritePosition();
            sprite.setRotate(rotation);

            // Add meteor trail effect if it's a meteor
            if (type == Type.METEOR && sprite.isVisible()) {
                createMeteorTrail();
            }

            logger.trace("Asteroid updated - Position: {}, Velocity: {}, Rotation: {}",
                    position, velocity, rotation);
        }
    }

    private void createMeteorTrail() {
        // Create a trail effect behind the meteor
        Canvas trailCanvas = new Canvas(baseWidth * 1.5, baseHeight * 1.5);
        GraphicsContext gc = trailCanvas.getGraphicsContext2D();

        // Calculate trail start position (behind the meteor)
        double trailLength = 30.0;
        double trailAngle = Math.atan2(velocity.getY(), velocity.getX());
        double trailStartX = position.getX() - Math.cos(trailAngle) * trailLength;
        double trailStartY = position.getY() - Math.sin(trailAngle) * trailLength;

        // Draw gradient trail
        gc.setFill(trailColor);
        for (int i = 0; i < 5; i++) {
            double alpha = 0.8 - (i * 0.2);
            gc.setFill(Color.rgb(255, 100, 0, alpha));
            gc.fillOval(
                    trailStartX + (i * Math.cos(trailAngle) * 5),
                    trailStartY + (i * Math.sin(trailAngle) * 5),
                    10 - i, 10 - i
            );
        }
    }

    public void hit() {
        if (isAlive && !isExploding) {
            logger.debug("{} hit at position: {}", type, position);
            explode();
        }
    }


    public List<Asteroid> split() {
        List<Asteroid> fragments = new ArrayList<>();
        if (type == Type.ASTEROID) {
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
            Asteroid fragment = new Asteroid(newPos, Type.ASTEROID);
            fragment.setInvulnerable(true);
            fragments.add(fragment);
        }

        logger.info("Asteroid split into {} fragments", fragments.size());}

        return fragments;
    }

    private void explode() {
        if (!isExploding) {
            isExploding = true;
            currentExplosionFrame = 0;

            // Calculate centered position for explosion
            double explosionX = position.getX() - (baseWidth * EXPLOSION_SCALE / 2);
            double explosionY = position.getY() - (baseHeight * EXPLOSION_SCALE / 2);

            // Set initial explosion position
            sprite.setTranslateX(explosionX);
            sprite.setTranslateY(explosionY);

            // Reset any rotation for the explosion
            sprite.setRotate(0);

            // Start explosion animation
            setupExplosionAnimation();
            explosionAnimation.play();
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
            String explodePath = (type == Type.ASTEROID) ? ASTEROID_EXPLODE_PATH : ASTEROID_EXPLODE_PATH;
            Image explodeSheet = new Image(getClass().getResourceAsStream(explodePath));

            // Calculate dimensions for each frame
            double frameWidth = explodeSheet.getWidth() / EXPLOSION_FRAMES;
            double frameHeight = explodeSheet.getHeight();

            // Scale explosion to be slightly larger than the original asteroid
            double scaledWidth = baseWidth * EXPLOSION_SCALE;
            double scaledHeight = baseHeight * EXPLOSION_SCALE;

            for (int i = 0; i < EXPLOSION_FRAMES; i++) {
                Canvas canvas = new Canvas(scaledWidth, scaledHeight);
                GraphicsContext gc = canvas.getGraphicsContext2D();

                // Enable better quality rendering
                gc.setImageSmoothing(true);

                // Calculate source rectangle from sprite sheet
                double srcX = i * frameWidth;
                double srcY = 0;

                // Draw the frame centered on the canvas with scaling
                gc.drawImage(explodeSheet,
                        srcX, srcY, frameWidth, frameHeight,  // source rectangle
                        0, 0, scaledWidth, scaledHeight       // destination rectangle
                );

                // Create snapshot with transparency
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

    private void setupExplosionAnimation() {
        explosionAnimation = new Timeline();

        // Create frames with scaling effect
        for (int i = 0; i < explosionFrames.size(); i++) {
            final int frameIndex = i;

            KeyFrame keyFrame = new KeyFrame(
                    FRAME_DURATION.multiply(i + 1),
                    e -> {
                        sprite.setImage(explosionFrames.get(frameIndex));

                        // Scale the sprite based on frame index
                        double progress = frameIndex / (double)(explosionFrames.size() - 1);
                        double currentScale = 1.0 + (progress * (EXPLOSION_SCALE - 1.0));

                        sprite.setFitWidth(baseWidth * currentScale);
                        sprite.setFitHeight(baseHeight * currentScale);

                        // Adjust position to keep explosion centered
                        double offsetX = (sprite.getFitWidth() - baseWidth) / 2;
                        double offsetY = (sprite.getFitHeight() - baseHeight) / 2;
                        sprite.setTranslateX(position.getX() - (baseWidth / 2) - offsetX);
                        sprite.setTranslateY(position.getY() - (baseHeight / 2) - offsetY);
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
    public Type getType() { return type; }
    public int getPoints() { return points; }
    public boolean isExploding() { return isExploding; }
    public double getRadius() { return type == Type.ASTEROID ? ASTEROID_RADIUS : METEOR_RADIUS; }

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
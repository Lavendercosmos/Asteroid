import javafx.application.Platform;
import javafx.geometry.Point2D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se233.asteroid.model.PlayerShip;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerShipTest {
    private PlayerShip playerShip;
    private static boolean jfxIsSetup;


    @BeforeAll
    public static void initJfxRuntime() throws InterruptedException {
        if (!jfxIsSetup) {
            // Initialize the JavaFX platform
            Platform.startup(() -> {});

            // Give JavaFX time to initialize
            Thread.sleep(500);
            jfxIsSetup = true;
        }
    }

    @BeforeEach
    void setUp() {
        // Run player ship creation on JavaFX thread
        Platform.runLater(() -> {
            playerShip = new PlayerShip(new Point2D(100, 100));
        });

        // Wait for initialization to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testMoveUp() {
        Point2D initialVelocity = playerShip.getVelocity();
        playerShip.moveUp();
        assertTrue(playerShip.getVelocity().getY() < initialVelocity.getY(),
                "Velocity should decrease on move up");
    }

    @Test
    void testMoveDown() {
        Point2D initialVelocity = playerShip.getVelocity();
        playerShip.moveDown();
        assertTrue(playerShip.getVelocity().getY() > initialVelocity.getY(),
                "Velocity should increase on move down");
    }

    @Test
    void testMoveLeft() {
        Point2D initialVelocity = playerShip.getVelocity();
        playerShip.moveLeft();
        assertTrue(playerShip.getVelocity().getX() < initialVelocity.getX(),
                "Velocity should decrease on move left");
    }

    @Test
    void testMoveRight() {
        Point2D initialVelocity = playerShip.getVelocity();
        playerShip.moveRight();
        assertTrue(playerShip.getVelocity().getX() > initialVelocity.getX(),
                "Velocity should increase on move right");
    }

    @Test
    void testRotation() {
        double initialRotation = playerShip.getRotation();
        playerShip.rotateLeft();
        assertEquals((initialRotation - 2.0 + 360) % 360, playerShip.getRotation(),
                "Rotation should decrease on rotate left");

        playerShip.rotateRight();
        assertEquals(initialRotation, playerShip.getRotation(),
                "Rotation should return to initial value after left and right rotation");
    }

    @Test
    void testShoot() {
        // Create a latch to wait for async operations
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        // Run shooting test on JavaFX thread
        Platform.runLater(() -> {
            try {
                var bullet = playerShip.shoot();
                assertNotNull(bullet, "Shooting should create a bullet");

                double radians = Math.toRadians(playerShip.getRotation() - 90);
                Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
                Point2D expectedPosition = playerShip.getPosition()
                        .add(direction.multiply(playerShip.sprite.getBoundsInLocal().getWidth() / 2));
                assertEquals(expectedPosition, bullet.getPosition(),
                        "Bullet should start at the expected position based on ship rotation");
            } finally {
                latch.countDown();
            }
        });

        try {
            // Wait with timeout
            if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                fail("Test timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testExplode() {
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                playerShip.explode();
                assertTrue(playerShip.isExploding,
                        "Ship should be in exploding state after explode");
                assertFalse(playerShip.isAlive(),
                        "Ship should not be alive after exploding");
            } finally {
                latch.countDown();
            }
        });

        try {
            if (!latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                fail("Test timed out");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
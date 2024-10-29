

import javafx.geometry.Point2D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se233.asteroid.model.PlayerShip;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerShipTest {
    private PlayerShip playerShip;

    @BeforeEach
    void setUp() {
        try {
            // Run on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                playerShip = new PlayerShip(new Point2D(100, 100));
            });
            // Wait for initialization to complete
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
        final boolean[] testComplete = new boolean[1];
        javafx.application.Platform.runLater(() -> {
            var bullet = playerShip.shoot();
            assertNotNull(bullet, "Shooting should create a bullet");

            double radians = Math.toRadians(playerShip.getRotation() - 90);
            Point2D direction = new Point2D(Math.cos(radians), Math.sin(radians));
            Point2D expectedPosition = playerShip.getPosition()
                    .add(direction.multiply(playerShip.sprite.getBoundsInLocal().getWidth() / 2));
            assertEquals(expectedPosition, bullet.getPosition(),
                    "Bullet should start at the expected position based on ship rotation");
            testComplete[0] = true;
        });

        // Wait for the test to complete
        try {
            while (!testComplete[0]) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testHit() {
        final boolean[] testComplete = new boolean[1];
        javafx.application.Platform.runLater(() -> {
            int initialLives = playerShip.getLives();
            playerShip.hit();
            assertEquals(initialLives - 1, playerShip.getLives(),
                    "Lives should decrease after a hit if ship is not invulnerable");
            assertTrue(playerShip.isInvulnerable(),
                    "Ship should become invulnerable after a hit");
            testComplete[0] = true;
        });

        // Wait for the test to complete
        try {
            while (!testComplete[0]) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testExplode() {
        final boolean[] testComplete = new boolean[1];
        javafx.application.Platform.runLater(() -> {
            playerShip.explode();
            assertTrue(playerShip.isExploding(),
                    "Ship should be in exploding state after explode");
            assertFalse(playerShip.isAlive(),
                    "Ship should not be alive after exploding");
            testComplete[0] = true;
        });

        // Wait for the test to complete
        try {
            while (!testComplete[0]) {
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

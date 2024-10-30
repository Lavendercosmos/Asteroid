import javafx.geometry.Point2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import se233.asteroid.model.Asteroid;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AsteroidTest {
    private Asteroid asteroid;

    @BeforeEach
    void setUp() {
        asteroid = new Asteroid(new Point2D(100, 100), Asteroid.Type.ASTEROID);
    }

    @Test
    void testInitialProperties() {
        assertEquals(Asteroid.Type.ASTEROID, asteroid.getType());
        assertFalse(asteroid.isExploding());
        assertFalse(asteroid.isInvulnerable());
        assertTrue(asteroid.isAlive());
    }

    @Test
    void testUpdatePositionAndRotation() {
        asteroid.update();
        Point2D initialPosition = asteroid.getPosition();
        double initialRotation = asteroid.getRotation();

        asteroid.update();

        // Ensure position changes according to velocity
        assertNotEquals(initialPosition, asteroid.getPosition());
        // Ensure rotation updates as expected
        assertNotEquals(initialRotation, asteroid.getRotation());
    }

    @Test
    void testHitTriggersExplosion() {
        asteroid.hit();
        assertTrue(asteroid.isExploding());
        assertFalse(asteroid.isAlive());
    }

    @Test
    void testSplitCreatesFragments() {
        asteroid.hit();
        List<Asteroid> fragments = asteroid.split();

        assertEquals(2, fragments.size());
        for (Asteroid fragment : fragments) {
            assertTrue(fragment.isInvulnerable());
            assertNotEquals(asteroid.getPosition(), fragment.getPosition());
        }
    }

    @Test
    void testInvulnerableState() {
        // Set invulnerable to true first
        asteroid.setInvulnerable(true);
        assertTrue(asteroid.isInvulnerable(), "Asteroid should be invulnerable after setting invulnerable to true");

        // Update once
        asteroid.update();
        assertTrue(asteroid.isInvulnerable(), "Asteroid should remain invulnerable after one update");

        // Optional: Test that invulnerability can be turned off
        asteroid.setInvulnerable(false);
        assertFalse(asteroid.isInvulnerable(), "Asteroid should not be invulnerable after setting invulnerable to false");
    }

    @Test
    void testCollisionDetection() {
        Asteroid anotherAsteroid = new Asteroid(new Point2D(100, 130), Asteroid.Type.ASTEROID);
        assertTrue(asteroid.collidesWith(anotherAsteroid));

        anotherAsteroid.setPosition(new Point2D(500, 500));
        assertFalse(asteroid.collidesWith(anotherAsteroid));
    }

    @Test
    void testDisposeStopsAnimation() {
        asteroid.hit();
        asteroid.dispose();
        assertFalse(asteroid.isAlive());
        assertNull(asteroid.getSprite().getImage());
    }
}
import javafx.geometry.Point2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import se233.asteroid.model.Enemy;
import se233.asteroid.model.EnemyBullet;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Enemy Character Tests")
public class EnemyTest {
    private Enemy regularEnemy;
    private Enemy secondTierEnemy;
    private final Point2D startPosition = new Point2D(400, 300);
    private final Point2D playerPosition = new Point2D(500, 300);

    @BeforeEach
    void setUp() {
        regularEnemy = new Enemy(startPosition, false);
        secondTierEnemy = new Enemy(startPosition, true);
    }

    @Test
    @DisplayName("Regular and Second Tier enemies should initialize with correct properties")
    void testInitialization() {
        // Test regular enemy
        assertFalse(regularEnemy.isSecondTier());
        assertEquals(15.0, regularEnemy.getHitRadius()); // DEFAULT_HITBOX
        assertTrue(regularEnemy.isAlive());
        assertFalse(regularEnemy.isExploding());

        // Test second tier enemy
        assertTrue(secondTierEnemy.isSecondTier());
        assertEquals(20.0, secondTierEnemy.getHitRadius()); // SECOND_TIER_HITBOX
        assertTrue(secondTierEnemy.isAlive());
        assertFalse(secondTierEnemy.isExploding());
    }

    @Test
    @DisplayName("Enemies should move correctly based on player position")
    void testMovement() {
        Point2D initialPosition = regularEnemy.getPosition();
        regularEnemy.updateAI(playerPosition);
        regularEnemy.update();

        Point2D newPosition = regularEnemy.getPosition();
        assertNotEquals(initialPosition, newPosition);
    }

    @Test
    @DisplayName("Enemies should maintain safe distance from player")
    void testDistanceManagement() {
        // Test too close
        Point2D tooClosePosition = new Point2D(401, 300); // Very close to enemy
        regularEnemy.updateAI(tooClosePosition);
        regularEnemy.update();

        double distanceAfterUpdate = regularEnemy.getPosition().distance(tooClosePosition);
        assertTrue(distanceAfterUpdate > regularEnemy.getPosition().distance(tooClosePosition));

        // Test too far
        Point2D tooFarPosition = new Point2D(800, 300);
        regularEnemy.updateAI(tooFarPosition);
        regularEnemy.update();

        distanceAfterUpdate = regularEnemy.getPosition().distance(tooFarPosition);
        assertTrue(distanceAfterUpdate < regularEnemy.getPosition().distance(tooFarPosition));
    }

    @Test
    @DisplayName("Enemy should shoot correctly")
    void testShooting() {
        // Update AI with player position to enable shooting
        regularEnemy.updateAI(playerPosition);

        // First shot should be possible
        EnemyBullet bullet = regularEnemy.enemyshoot();
        assertNotNull(bullet);

        // Immediate second shot should not be possible (cooldown)
        EnemyBullet secondBullet = regularEnemy.enemyshoot();
        assertNull(secondBullet);
    }

    @Test
    @DisplayName("Second tier enemy should shoot faster than regular enemy")
    void testShootingSpeed() {
        regularEnemy.updateAI(playerPosition);
        secondTierEnemy.updateAI(playerPosition);

        // Both enemies shoot
        EnemyBullet regularBullet = regularEnemy.enemyshoot();
        EnemyBullet secondTierBullet = secondTierEnemy.enemyshoot();

        assertNotNull(regularBullet);
        assertNotNull(secondTierBullet);

        // Update multiple times to simulate time passing
        for (int i = 0; i < 90; i++) { // ~1.5 seconds at 60 FPS
            regularEnemy.update();
            secondTierEnemy.update();
        }

        // Second tier should be able to shoot again, regular should still be on cooldown
        assertNull(regularEnemy.enemyshoot());
        assertNotNull(secondTierEnemy.enemyshoot());
    }

    @Test
    @DisplayName("Enemy should handle hit and explosion correctly")
    void testHitAndExplosion() {
        assertTrue(regularEnemy.isAlive());
        assertFalse(regularEnemy.isExploding());

        regularEnemy.hit();

        assertTrue(regularEnemy.isExploding());
        assertEquals(0, regularEnemy.getCurrentExplosionFrame());

        // Simulate updates to progress explosion animation
        for (int i = 0; i < 60; i++) { // 1 second worth of updates
            regularEnemy.update();
        }

        assertFalse(regularEnemy.isAlive());
    }

    @Test
    @DisplayName("Enemy should stop moving when hit")
    void testMovementAfterHit() {
        regularEnemy.updateAI(playerPosition);
        regularEnemy.update();
        Point2D positionBeforeHit = regularEnemy.getPosition();

        regularEnemy.hit();
        regularEnemy.update();
        Point2D positionAfterHit = regularEnemy.getPosition();

        assertEquals(positionBeforeHit, positionAfterHit);
    }

    @Test
    @DisplayName("Enemy should maintain correct rotation based on movement")
    void testRotation() {
        Point2D targetPosition = new Point2D(500, 300); // Target to the right
        regularEnemy.moveTowards(targetPosition);
        regularEnemy.update();

        double expectedRotation = 0.0; // Facing right
        double actualRotation = regularEnemy.getRotation();

        // Allow for small floating-point differences
        assertTrue(Math.abs(expectedRotation - actualRotation) < 1.0);
    }

    @Test
    @DisplayName("Enemy should handle manual velocity changes")
    void testVelocityControl() {
        Point2D newVelocity = new Point2D(1.0, 0.0);
        regularEnemy.setVelocity(newVelocity);

        // Velocity should be normalized and scaled by speed
        Point2D currentVelocity = regularEnemy.getVelocity();
        assertEquals(1.0, currentVelocity.normalize().getX(), 0.01);
        assertEquals(0.0, currentVelocity.normalize().getY(), 0.01);
    }

    @Test
    @DisplayName("Dead enemy should not shoot or move")
    void testDeadEnemyBehavior() {
        regularEnemy.hit();
        // Wait for explosion to complete
        for (int i = 0; i < 120; i++) { // 2 seconds worth of updates
            regularEnemy.update();
        }

        Point2D positionBeforeUpdate = regularEnemy.getPosition();
        regularEnemy.updateAI(playerPosition);
        regularEnemy.update();

        // Position should not change
        assertEquals(positionBeforeUpdate, regularEnemy.getPosition());
        // Should not be able to shoot
        assertNull(regularEnemy.enemyshoot());
    }
}
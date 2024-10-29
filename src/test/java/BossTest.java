import javafx.geometry.Point2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import se233.asteroid.model.Boss;
import se233.asteroid.model.Enemy;
import se233.asteroid.model.Bullet;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Boss Character Tests")
public class BossTest {
    private Boss boss;
    private final Point2D startPosition = new Point2D(400, 300);
    private final int testWave = 2;

    @BeforeEach
    void setUp() {
        boss = new Boss(startPosition, testWave);
    }

    @Test
    @DisplayName("Boss should initialize with correct health based on wave")
    void testInitialHealth() {
        int expectedHealth = 50 * testWave; // BASE_HEALTH * wave
        assertEquals(expectedHealth, boss.getMaxHealth());
        assertEquals(expectedHealth, boss.getHealth());
        assertEquals(1.0, boss.getHealthPercentage());
    }

    @Test
    @DisplayName("Boss should take damage correctly")
    void testTakeDamage() {
        int initialHealth = boss.getHealth();
        int damage = 20;
        boss.hit(damage);

        assertEquals(initialHealth - damage, boss.getHealth());
        assertTrue(boss.getHealthPercentage() < 1.0);
        assertTrue(boss.isAlive());
    }

    @Test
    @DisplayName("Boss should enter enraged state at 30% health")
    void testEnragedState() {
        int damageToEnrage = (int)(boss.getMaxHealth() * 0.75); // Damage to get below 30% health
        assertFalse(boss.isEnraged());

        boss.hit(damageToEnrage);

        assertTrue(boss.isEnraged());
        assertTrue(boss.getHealth() > 0);
        assertTrue(boss.getHealthPercentage() <= 0.3);
    }

    @Test
    @DisplayName("Boss should die when health reaches zero")
    void testDeath() {
        assertTrue(boss.isAlive());

        boss.hit(boss.getMaxHealth()); // Deal maximum health as damage

        assertEquals(0, boss.getHealth());
        assertFalse(boss.isAlive());
    }

    @Test
    @DisplayName("Boss should spawn enemies when hit multiple times")
    void testEnemySpawning() {
        // Hit boss enough times to trigger spawning
        for (int i = 0; i < 5; i++) {
            boss.hit(10);
        }

        List<Enemy> spawnedEnemies = boss.collectSpawnedEnemies();
        assertFalse(spawnedEnemies.isEmpty());
    }

    @Test
    @DisplayName("Boss should perform special attack correctly")
    void testSpecialAttack() {
        Bullet[] bullets = boss.shootSpecialAttack();

        assertEquals(8, bullets.length); // SPECIAL_ATTACK_BULLET_COUNT

        // Check that bullets are spawned in different directions
        Point2D firstBulletDir = bullets[0].getVelocity();
        Point2D lastBulletDir = bullets[bullets.length - 1].getVelocity();
        assertNotEquals(firstBulletDir, lastBulletDir);

        // Check that bullets start at boss position
        for (Bullet bullet : bullets) {
            assertEquals(boss.getPosition(), bullet.getPosition());
        }
    }

    @Test
    @DisplayName("Boss should update position based on movement pattern")
    void testMovementPattern() {
        Point2D initialPos = boss.getPosition();
        boss.update(0.016, new Point2D(0, 0)); // Simulate one frame update

        assertNotEquals(initialPos, boss.getPosition());
    }

    @Test
    @DisplayName("Boss should chase player when enraged")
    void testEnragedChase() {
        // Get boss to enraged state
        boss.hit((int)(boss.getMaxHealth() * 0.75));
        assertTrue(boss.isEnraged());

        Point2D playerPos = new Point2D(500, 500);
        Point2D initialPos = boss.getPosition();

        boss.update(0.016, playerPos);

        Point2D newPos = boss.getPosition();
        Point2D moveDirection = newPos.subtract(initialPos).normalize();
        Point2D expectedDirection = playerPos.subtract(initialPos).normalize();

        // Check if boss is moving towards player (allowing for small floating-point differences)
        assertTrue(moveDirection.distance(expectedDirection) < 0.01);
    }

    @Test
    @DisplayName("Boss should not spawn enemies when at critical health")
    void testNoSpawningAtCriticalHealth() {
        // Damage boss to critical health (below 10%)
        boss.hit((int)(boss.getMaxHealth() * 0.95));

        // Try to trigger spawning
        for (int i = 0; i < 5; i++) {
            boss.hit(1);
        }

        List<Enemy> spawnedEnemies = boss.collectSpawnedEnemies();
        assertTrue(spawnedEnemies.isEmpty());
    }

    @Test
    @DisplayName("Boss wave number should be immutable")
    void testWaveImmutable() {
        int initialWave = boss.getWave();
        boss.hit(20);
        boss.update(0.016, new Point2D(0, 0));
        assertEquals(initialWave, boss.getWave());
    }
}
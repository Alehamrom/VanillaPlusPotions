package ru.vanillaplus.potions;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MagnetismMathTest {

    @Test
    public void testVectorCalculationTowardsPlayer() {
        // Player at (10, 65, 10), pull target at (10, 65.6, 10)
        Vector target = new Vector(10.0, 65.6, 10.0);
        // Item at (10, 64, 13) -> distance ~ 3.4
        Vector itemLoc = new Vector(10.0, 64.0, 13.0);

        Vector delta = target.clone().subtract(itemLoc);
        double dist = delta.length();
        assertTrue(dist > 0.25 && dist <= 6.0, "Item should be within level 1 magnetism radius");

        double pullSpeed = 0.38;
        Vector velocity = delta.normalize().multiply(pullSpeed);

        assertEquals(pullSpeed, velocity.length(), 1e-4);
        // Should move in positive Y direction and negative Z direction
        assertTrue(velocity.getY() > 0);
        assertTrue(velocity.getZ() < 0);
        assertEquals(0.0, velocity.getX(), 1e-4);
    }

    @Test
    public void testGroundLift() {
        Vector velocity = new Vector(0.38, 0.05, 0.0);
        boolean onGround = true;
        if (onGround) {
            velocity.setY(Math.max(velocity.getY(), 0.18));
        }
        assertEquals(0.18, velocity.getY(), 1e-4);
    }

    @Test
    public void testRadiusFiltering() {
        double radius = 6.0;
        Vector target = new Vector(0, 0, 0);
        Vector itemNear = new Vector(3, 0, 4); // distance 5.0 <= 6.0
        Vector itemFar = new Vector(5, 0, 5);  // distance 7.07 > 6.0
        Vector itemTooClose = new Vector(0.1, 0, 0.1); // distance ~0.14 < 0.25

        assertTrue(target.distance(itemNear) <= radius && target.distance(itemNear) >= 0.25);
        assertFalse(target.distance(itemFar) <= radius);
        assertFalse(target.distance(itemTooClose) >= 0.25);
    }
}

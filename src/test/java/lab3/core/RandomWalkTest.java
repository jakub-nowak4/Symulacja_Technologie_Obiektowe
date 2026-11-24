package lab3.core;

import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomWalkTest {
    private static final double DELTA = 1e-9;

    private static SimulationConfig config(double changeProbability, double maxTurn, double maxSpeedDelta) {
        return new SimulationConfig(30, 30, 60, 0, 1.8, 0.3, 0.0, 0.5, changeProbability, maxTurn, maxSpeedDelta);
    }

    private static Motion start(double speed, double heading) {
        return new Motion(new Vector2D(10, 10), Vector2D.fromPolar(speed, heading));
    }

    /** Angle between two headings in [0, pi], independent of wrapping. */
    private static double angleBetween(double a, double b) {
        double d = Math.abs(a - b) % (2 * Math.PI);
        return d > Math.PI ? 2 * Math.PI - d : d;
    }

    @Test
    void withoutChangeMovesStraightByVelocityTimesDt() {
        Motion m = start(2.0, 0.0);

        Motion next = RandomWalk.next(m, new SplitMix64Rng(1), config(0.0, 0.5, 0.5));

        assertEquals(10.0 + 2.0 * Ticks.DT, next.position().getX(), DELTA);
        assertEquals(10.0, next.position().getY(), DELTA);
        assertEquals(2.0, next.speed(), DELTA);
        assertEquals(0.0, next.heading(), DELTA);
    }

    @Test
    void maxSpeedMovesTenCentimetersPerTick() {
        Motion next = RandomWalk.next(start(2.5, 0.0), new SplitMix64Rng(1), config(0.0, 0.5, 0.5));

        assertEquals(0.1, next.position().minus(new Vector2D(10, 10)).abs(), DELTA);
    }

    @Test
    void usesOneDrawWhenVelocityDoesNotChange() {
        ScriptedRng rng = new ScriptedRng(0.9);

        RandomWalk.next(start(1.0, 0.0), rng, config(0.5, 0.5, 0.5));

        assertEquals(1, rng.state());
    }

    @Test
    void usesThreeDrawsWhenVelocityChanges() {
        ScriptedRng rng = new ScriptedRng(0.1, 0.5, 0.5);

        RandomWalk.next(start(1.0, 0.0), rng, config(0.5, 0.5, 0.5));

        assertEquals(3, rng.state());
    }

    @Test
    void changeTurnsHeadingByScriptedAngle() {
        double maxTurn = Math.PI / 4;
        ScriptedRng rng = new ScriptedRng(0.0, 0.75, 0.5);

        Motion next = RandomWalk.next(start(1.0, 0.0), rng, config(1.0, maxTurn, 0.5));

        // turn = -pi/4 + 0.75 * (pi/2) = pi/8
        assertEquals(Math.PI / 8, next.heading(), DELTA);
        assertEquals(1.0, next.speed(), DELTA);
    }

    @Test
    void changeAltersSpeedByScriptedDelta() {
        ScriptedRng rng = new ScriptedRng(0.0, 0.5, 0.0);

        Motion next = RandomWalk.next(start(1.0, 0.0), rng, config(1.0, 0.5, 0.5));

        // delta = -0.5 + 0.0 * 1.0 = -0.5
        assertEquals(0.5, next.speed(), DELTA);
    }

    @Test
    void speedIsClampedToMinSpeed() {
        ScriptedRng rng = new ScriptedRng(0.0, 0.5, 0.0);

        Motion next = RandomWalk.next(start(0.4, 0.0), rng, config(1.0, 0.5, 0.5));

        assertEquals(0.3, next.speed(), DELTA);
    }

    @Test
    void speedIsClampedToMaxSpeed() {
        ScriptedRng rng = new ScriptedRng(0.0, 0.5, 0.99);

        Motion next = RandomWalk.next(start(2.4, 0.0), rng, config(1.0, 0.5, 0.5));

        assertEquals(2.5, next.speed(), DELTA);
    }

    @Test
    void positionAdvancesAlongTheNewVelocity() {
        ScriptedRng rng = new ScriptedRng(0.0, 0.75, 0.5);

        Motion next = RandomWalk.next(start(1.0, 0.0), rng, config(1.0, Math.PI / 2, 0.5));

        // turn = -pi/2 + 0.75 * pi = pi/4
        double expectedStep = 1.0 * Ticks.DT;
        assertEquals(10.0 + expectedStep * Math.cos(Math.PI / 4), next.position().getX(), DELTA);
        assertEquals(10.0 + expectedStep * Math.sin(Math.PI / 4), next.position().getY(), DELTA);
    }

    @Test
    void speedNeverLeavesAllowedRangeOverManySteps() {
        SimulationConfig config = config(1.0, Math.PI, 5.0);
        Rng rng = new SplitMix64Rng(2024);
        Motion m = start(1.0, 0.0);

        for (int i = 0; i < 100_000; i++) {
            m = RandomWalk.next(m, rng, config);
            assertTrue(m.speed() <= SimulationConfig.MAX_SPEED + DELTA, "too fast: " + m.speed());
            assertTrue(m.speed() >= config.minSpeed() - DELTA, "too slow: " + m.speed());
        }
    }

    @Test
    void singleChangeNeverExceedsTurnAndSpeedLimits() {
        double maxTurn = Math.PI / 6;
        double maxSpeedDelta = 0.4;
        SimulationConfig config = config(1.0, maxTurn, maxSpeedDelta);
        Rng rng = new SplitMix64Rng(7);

        for (int i = 0; i < 10_000; i++) {
            Motion before = start(1.0 + rng.nextDouble(), rng.range(-Math.PI, Math.PI));
            Motion after = RandomWalk.next(before, rng, config);

            assertTrue(angleBetween(before.heading(), after.heading()) <= maxTurn + DELTA);
            assertTrue(Math.abs(after.speed() - before.speed()) <= maxSpeedDelta + DELTA);
        }
    }

    @Test
    void sameSeedGivesSameTrajectory() {
        SimulationConfig config = config(0.05, Math.PI / 4, 0.5);
        Rng a = new SplitMix64Rng(99);
        Rng b = new SplitMix64Rng(99);
        Motion ma = start(1.5, 0.3);
        Motion mb = start(1.5, 0.3);

        for (int i = 0; i < 1000; i++) {
            ma = RandomWalk.next(ma, a, config);
            mb = RandomWalk.next(mb, b, config);
        }

        assertEquals(ma.position().getX(), mb.position().getX(), 0.0);
        assertEquals(ma.position().getY(), mb.position().getY(), 0.0);
        assertEquals(a.state(), b.state());
    }

    @Test
    void doesNotModifyTheInputMotion() {
        Motion m = start(1.0, 0.0);

        RandomWalk.next(m, new SplitMix64Rng(5), config(1.0, 1.0, 0.5));

        assertEquals(10.0, m.position().getX(), 0.0);
        assertEquals(1.0, m.speed(), DELTA);
    }
}

package lab3.core;

import lab3.core.BoundaryResult.Outcome;
import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundaryPolicyTest {
    private static final double DELTA = 1e-9;
    private static final Area AREA = new Area(30, 20);
    private static final BoundaryPolicy POLICY = new BoundaryPolicy(AREA);

    private static Motion motion(double x, double y, double vx, double vy) {
        return new Motion(new Vector2D(x, y), new Vector2D(vx, vy));
    }

    private static void assertMotion(double x, double y, double vx, double vy, Motion m) {
        assertEquals(x, m.position().getX(), DELTA);
        assertEquals(y, m.position().getY(), DELTA);
        assertEquals(vx, m.velocity().getX(), DELTA);
        assertEquals(vy, m.velocity().getY(), DELTA);
    }

    @Test
    void personInsideTheAreaIsLeftAloneAndUsesNoDraw() {
        ScriptedRng rng = new ScriptedRng(0.1);
        Motion m = motion(10, 10, 1, 1);

        BoundaryResult result = POLICY.apply(m, rng);

        assertEquals(Outcome.INSIDE, result.outcome());
        assertSame(m, result.motion());
        assertEquals(0, rng.state());
    }

    @Test
    void personOnTheBorderIsNotAffected() {
        ScriptedRng rng = new ScriptedRng(0.1);

        assertEquals(Outcome.INSIDE, POLICY.apply(motion(0, 10, 1, 0), rng).outcome());
        assertEquals(Outcome.INSIDE, POLICY.apply(motion(0, 10, 0, 1), rng).outcome());
        assertEquals(Outcome.INSIDE, POLICY.apply(motion(0, 10, -1, 0), rng).outcome());
        assertEquals(0, rng.state());
    }

    @Test
    void personOutsideButMovingInwardIsNotAffected() {
        ScriptedRng rng = new ScriptedRng(0.1);

        assertEquals(Outcome.INSIDE, POLICY.apply(motion(-0.05, 10, 1, 0), rng).outcome());
        assertEquals(0, rng.state());
    }

    @Test
    void justBelowHalfBouncesOnTheLeftSide() {
        BoundaryResult result = POLICY.apply(motion(-0.05, 10, -1, 0.5), new ScriptedRng(0.49));

        assertEquals(Outcome.BOUNCED, result.outcome());
        assertMotion(0, 10, 1, 0.5, result.motion());
    }

    @Test
    void halfAndAboveExits() {
        BoundaryResult result = POLICY.apply(motion(-0.05, 10, -1, 0.5), new ScriptedRng(0.50));

        assertEquals(Outcome.EXITED, result.outcome());
    }

    @Test
    void bouncesOnTheRightBottomAndTopSides() {
        assertMotion(30, 10, -2, 1,
                POLICY.apply(motion(30.07, 10, 2, 1), new ScriptedRng(0.1)).motion());
        assertMotion(5, 0, 1, 3,
                POLICY.apply(motion(5, -0.02, 1, -3), new ScriptedRng(0.1)).motion());
        assertMotion(5, 20, 1, -3,
                POLICY.apply(motion(5, 20.09, 1, 3), new ScriptedRng(0.1)).motion());
    }

    @Test
    void cornerIsOneDecisionForBothAxes() {
        ScriptedRng rng = new ScriptedRng(0.1);

        BoundaryResult result = POLICY.apply(motion(-0.02, -0.03, -1, -2), rng);

        assertEquals(Outcome.BOUNCED, result.outcome());
        assertMotion(0, 0, 1, 2, result.motion());
        assertEquals(1, rng.state());
    }

    @Test
    void cornerExitsAsAWhole() {
        ScriptedRng rng = new ScriptedRng(0.9);

        BoundaryResult result = POLICY.apply(motion(30.01, 20.02, 1, 2), rng);

        assertEquals(Outcome.EXITED, result.outcome());
        assertEquals(1, rng.state());
    }

    @Test
    void crossingOneAxisLeavesTheOtherUntouched() {
        BoundaryResult result = POLICY.apply(motion(-0.01, 10, -1, -1), new ScriptedRng(0.1));

        assertMotion(0, 10, 1, -1, result.motion());
    }

    @Test
    void decisionUsesExactlyOneDraw() {
        ScriptedRng rng = new ScriptedRng(0.3);

        POLICY.apply(motion(-0.05, 10, -1, 0), rng);

        assertEquals(1, rng.state());
    }

    @Test
    void bouncePreservesSpeed() {
        Motion m = motion(30.04, 5, 1.2, -0.7);

        BoundaryResult result = POLICY.apply(m, new ScriptedRng(0.1));

        assertEquals(m.speed(), result.motion().speed(), DELTA);
    }

    @Test
    void aBouncedPersonDoesNotTriggerTheDecisionAgain() {
        BoundaryResult first = POLICY.apply(motion(-0.05, 10, -1, 0), new ScriptedRng(0.1));
        ScriptedRng rng = new ScriptedRng(0.1);

        BoundaryResult second = POLICY.apply(first.motion(), rng);

        assertEquals(Outcome.INSIDE, second.outcome());
        assertEquals(0, rng.state());
    }

    @Test
    void aboutHalfOfThoseReachingTheBorderBounce() {
        Rng rng = new SplitMix64Rng(5);
        int bounced = 0;
        int n = 100_000;

        for (int i = 0; i < n; i++) {
            if (POLICY.apply(motion(-0.05, 10, -1, 0), rng).outcome() == Outcome.BOUNCED) {
                bounced++;
            }
        }

        assertEquals(0.5, (double) bounced / n, 0.01);
    }

    @Test
    void walkingPersonNeverStaysOutsideTheArea() {
        SimulationConfig config = SimulationConfig.noImmunity();
        Rng rng = new SplitMix64Rng(2026);
        Motion m = new Motion(new Vector2D(15, 10), Vector2D.fromPolar(2.0, 0.7));
        int bounces = 0;
        int exits = 0;

        for (int i = 0; i < 300_000; i++) {
            m = RandomWalk.next(m, rng, config);
            BoundaryResult result = POLICY.apply(m, rng);
            if (result.outcome() == Outcome.EXITED) {
                exits++;
                Vector2D entry = AREA.randomBoundaryPoint(rng);
                Vector2D velocity = AREA.directInward(entry, Vector2D.fromPolar(rng.range(0.3, 2.5), rng.range(0, 2 * Math.PI)));
                m = new Motion(entry, velocity);
            } else {
                if (result.outcome() == Outcome.BOUNCED) {
                    bounces++;
                }
                m = result.motion();
            }
            assertTrue(AREA.contains(m.position()), "outside at step " + i + ": " + m.position().getX() + ", " + m.position().getY());
        }

        assertTrue(bounces > 0, "expected at least one bounce");
        assertTrue(exits > 0, "expected at least one exit");
    }
}

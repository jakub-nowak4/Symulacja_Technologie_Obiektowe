package lab3.core;

import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaTest {
    private static final double DELTA = 1e-9;
    private static final Area AREA = new Area(30, 20);

    private static void assertVector(double x, double y, Vector2D v) {
        assertEquals(x, v.getX(), DELTA);
        assertEquals(y, v.getY(), DELTA);
    }

    private static boolean onBorder(Area area, Vector2D p) {
        return p.getX() == 0 || p.getX() == area.width() || p.getY() == 0 || p.getY() == area.height();
    }

    @Test
    void perimeterIsTwiceTheSumOfSides() {
        assertEquals(100.0, AREA.perimeter(), DELTA);
    }

    @Test
    void containsIncludesTheBorder() {
        assertTrue(AREA.contains(new Vector2D(0, 0)));
        assertTrue(AREA.contains(new Vector2D(30, 20)));
        assertTrue(AREA.contains(new Vector2D(15, 10)));
        assertFalse(AREA.contains(new Vector2D(-0.001, 10)));
        assertFalse(AREA.contains(new Vector2D(30.001, 10)));
        assertFalse(AREA.contains(new Vector2D(10, -0.001)));
        assertFalse(AREA.contains(new Vector2D(10, 20.001)));
    }

    @Test
    void rejectsInvalidSize() {
        assertThrows(IllegalArgumentException.class, () -> new Area(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new Area(10, -1));
        assertThrows(IllegalArgumentException.class, () -> new Area(Double.NaN, 10));
        assertThrows(IllegalArgumentException.class, () -> new Area(10, Double.POSITIVE_INFINITY));
    }

    @Test
    void boundaryPointWalksTheFourSidesInOrder() {
        assertVector(5, 0, AREA.randomBoundaryPoint(new ScriptedRng(0.05)));     // bottom
        assertVector(30, 5, AREA.randomBoundaryPoint(new ScriptedRng(0.35)));    // right
        assertVector(25, 20, AREA.randomBoundaryPoint(new ScriptedRng(0.55)));   // top
        assertVector(0, 15, AREA.randomBoundaryPoint(new ScriptedRng(0.85)));    // left
    }

    @Test
    void boundaryPointCanBeACorner() {
        assertVector(0, 0, AREA.randomBoundaryPoint(new ScriptedRng(0.0)));
    }

    @Test
    void boundaryPointUsesExactlyOneDraw() {
        ScriptedRng rng = new ScriptedRng(0.3);

        AREA.randomBoundaryPoint(rng);

        assertEquals(1, rng.state());
    }

    @Test
    void boundaryPointsAlwaysLieOnTheBorderInsideTheArea() {
        Rng rng = new SplitMix64Rng(31);

        for (int i = 0; i < 100_000; i++) {
            Vector2D p = AREA.randomBoundaryPoint(rng);
            assertTrue(AREA.contains(p), "outside: " + p.getX() + ", " + p.getY());
            assertTrue(onBorder(AREA, p), "not on border: " + p.getX() + ", " + p.getY());
        }
    }

    @Test
    void sidesAreChosenProportionallyToTheirLength() {
        Rng rng = new SplitMix64Rng(2025);
        int n = 200_000;
        int bottom = 0;
        int right = 0;
        int top = 0;
        int left = 0;

        for (int i = 0; i < n; i++) {
            Vector2D p = AREA.randomBoundaryPoint(rng);
            if (p.getY() == 0) {
                bottom++;
            } else if (p.getX() == 30) {
                right++;
            } else if (p.getY() == 20) {
                top++;
            } else {
                left++;
            }
        }

        assertEquals(0.30, (double) bottom / n, 0.01);
        assertEquals(0.20, (double) right / n, 0.01);
        assertEquals(0.30, (double) top / n, 0.01);
        assertEquals(0.20, (double) left / n, 0.01);
    }

    @Test
    void interiorPointUsesXThenYAndTwoDraws() {
        ScriptedRng rng = new ScriptedRng(0.5, 0.25);

        Vector2D p = AREA.randomInteriorPoint(rng);

        assertVector(15, 5, p);
        assertEquals(2, rng.state());
    }

    @Test
    void interiorPointsStayInsideTheArea() {
        Rng rng = new SplitMix64Rng(8);

        for (int i = 0; i < 50_000; i++) {
            assertTrue(AREA.contains(AREA.randomInteriorPoint(rng)));
        }
    }

    @Test
    void directInwardFlipsOutwardComponentOnEachSide() {
        assertVector(1, 2, AREA.directInward(new Vector2D(0, 10), new Vector2D(-1, 2)));     // left
        assertVector(-1, 0.5, AREA.directInward(new Vector2D(30, 10), new Vector2D(1, 0.5))); // right
        assertVector(1, 2, AREA.directInward(new Vector2D(10, 0), new Vector2D(1, -2)));     // bottom
        assertVector(1, -2, AREA.directInward(new Vector2D(10, 20), new Vector2D(1, 2)));    // top
    }

    @Test
    void directInwardKeepsVelocityThatAlreadyPointsInside() {
        assertVector(1, 2, AREA.directInward(new Vector2D(0, 10), new Vector2D(1, 2)));
        assertVector(-1, -2, AREA.directInward(new Vector2D(30, 20), new Vector2D(-1, -2)));
    }

    @Test
    void directInwardKeepsComponentAlongTheBorder() {
        assertVector(0, 3, AREA.directInward(new Vector2D(0, 10), new Vector2D(0, 3)));
        assertVector(-2, 0, AREA.directInward(new Vector2D(10, 0), new Vector2D(-2, 0)));
    }

    @Test
    void directInwardHandlesCorners() {
        assertVector(1, 1, AREA.directInward(new Vector2D(0, 0), new Vector2D(-1, -1)));
        assertVector(1, 1, AREA.directInward(new Vector2D(0, 0), new Vector2D(1, -1)));
        assertVector(-1, -1, AREA.directInward(new Vector2D(30, 20), new Vector2D(1, 1)));
        assertVector(-1, 1, AREA.directInward(new Vector2D(30, 0), new Vector2D(1, -1)));
    }

    @Test
    void directInwardRejectsPointsNotOnTheBorder() {
        assertThrows(IllegalArgumentException.class,
                () -> AREA.directInward(new Vector2D(10, 10), new Vector2D(1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> AREA.directInward(new Vector2D(-1, 10), new Vector2D(1, 1)));
    }

    @Test
    void anEntrantDirectedInwardNeverStartsMovingOutOfTheArea() {
        Rng rng = new SplitMix64Rng(77);

        for (int i = 0; i < 20_000; i++) {
            Vector2D p = AREA.randomBoundaryPoint(rng);
            Vector2D v = Vector2D.fromPolar(rng.range(0.3, 2.5), rng.range(0, 2 * Math.PI));

            Vector2D inward = AREA.directInward(p, v);

            assertTrue(AREA.contains(p.plus(inward.times(1e-6))), "leaves area from " + p.getX() + ", " + p.getY());
            assertEquals(v.abs(), inward.abs(), DELTA);
        }
    }
}

package lab3.ui.draw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldTransformTest {
    private static final double DELTA = 1e-9;

    @Test
    void squareAreaInSquareCanvasFillsItExceptForTheMargin() {
        WorldTransform t = new WorldTransform(30, 30, 640, 640, 20);

        assertEquals(20.0, t.scale(), DELTA);     // (640 - 40) / 30
        assertEquals(20, t.toPixelX(0), DELTA);
        assertEquals(20, t.toPixelY(0), DELTA);
        assertEquals(620, t.toPixelX(30), DELTA);
        assertEquals(620, t.toPixelY(30), DELTA);
    }

    @Test
    void areaIsCentredInAWideCanvas() {
        WorldTransform t = new WorldTransform(30, 30, 1000, 340, 20);

        assertEquals(10.0, t.scale(), DELTA);     // limited by the height: (340 - 40) / 30
        assertEquals(350, t.areaLeft(), DELTA);   // (1000 - 300) / 2
        assertEquals(20, t.areaTop(), DELTA);
        assertEquals(500, t.toPixelX(15), DELTA); // the middle of the area is the middle of the canvas
        assertEquals(170, t.toPixelY(15), DELTA);
    }

    @Test
    void areaIsCentredInATallCanvas() {
        WorldTransform t = new WorldTransform(30, 30, 340, 1000, 20);

        assertEquals(10.0, t.scale(), DELTA);
        assertEquals(20, t.areaLeft(), DELTA);
        assertEquals(350, t.areaTop(), DELTA);
    }

    @Test
    void keepsTheProportionsOfARectangularArea() {
        WorldTransform t = new WorldTransform(50, 20, 600, 600, 0);

        assertEquals(12.0, t.scale(), DELTA);
        assertEquals(600, t.areaPixelWidth(), DELTA);
        assertEquals(240, t.areaPixelHeight(), DELTA);
        assertEquals(180, t.areaTop(), DELTA);
        assertEquals(t.areaPixelWidth() / t.areaPixelHeight(), 50.0 / 20.0, DELTA);
    }

    @Test
    void lengthsAreScaledWithoutOffset() {
        WorldTransform t = new WorldTransform(30, 30, 640, 640, 20);

        assertEquals(40.0, t.toPixels(2.0), DELTA);   // the 2 m contact radius
        assertEquals(0.0, t.toPixels(0.0), DELTA);
    }

    @Test
    void everyPointOfTheAreaIsInsideTheCanvas() {
        WorldTransform t = new WorldTransform(30, 20, 800, 500, 25);

        for (double x = 0; x <= 30; x += 7.5) {
            for (double y = 0; y <= 20; y += 5) {
                assertTrue(t.toPixelX(x) >= 25 - DELTA && t.toPixelX(x) <= 800 - 25 + DELTA);
                assertTrue(t.toPixelY(y) >= 25 - DELTA && t.toPixelY(y) <= 500 - 25 + DELTA);
            }
        }
    }

    @Test
    void canvasSmallerThanTheMarginsShowsNothingButDoesNotFail() {
        WorldTransform t = new WorldTransform(30, 30, 30, 30, 20);

        assertEquals(0.0, t.scale(), DELTA);
        assertEquals(0.0, t.toPixels(2.0), DELTA);
        assertEquals(15, t.toPixelX(10), DELTA);   // everything collapses to the centre
        assertEquals(new WorldTransform(30, 30, 0, 0, 0).scale(), 0.0, DELTA);
    }

    @Test
    void rejectsWrongSizes() {
        assertThrows(IllegalArgumentException.class, () -> new WorldTransform(0, 30, 100, 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorldTransform(30, -1, 100, 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorldTransform(Double.NaN, 30, 100, 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorldTransform(30, 30, -1, 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new WorldTransform(30, 30, 100, 100, -1));
        assertThrows(IllegalArgumentException.class, () -> new WorldTransform(30, 30, Double.NaN, 100, 0));
    }
}

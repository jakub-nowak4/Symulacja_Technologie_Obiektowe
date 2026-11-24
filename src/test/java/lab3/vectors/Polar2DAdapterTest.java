package lab3.vectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Polar2DAdapterTest {
    private static final double DELTA = 1e-10;

    @Test
    void getAngleForPositiveXAxisIsZero() {
        assertEquals(0.0, new Polar2DAdapter(new Vector2D(1, 0)).getAngle(), DELTA);
    }

    @Test
    void getAngleForPositiveYAxisIsPiOverTwo() {
        assertEquals(Math.PI / 2, new Polar2DAdapter(new Vector2D(0, 1)).getAngle(), DELTA);
    }

    @Test
    void getAngleForNegativeXAxisIsPi() {
        assertEquals(Math.PI, new Polar2DAdapter(new Vector2D(-1, 0)).getAngle(), DELTA);
    }

    @Test
    void getAngleForOneOneIsPiOverFour() {
        assertEquals(Math.PI / 4, new Polar2DAdapter(new Vector2D(1, 1)).getAngle(), DELTA);
    }

    @Test
    void getAngleForThreeFourIsApproximatelyZeroPoint9273() {
        assertEquals(0.927295218, new Polar2DAdapter(new Vector2D(3, 4)).getAngle(), DELTA);
    }

    @Test
    void getAngleForNegativeYAxisIsMinusPiOverTwo() {
        assertEquals(-Math.PI / 2, new Polar2DAdapter(new Vector2D(0, -1)).getAngle(), DELTA);
    }

    @Test
    void absAndComponentsAreDelegated() {
        Polar2DAdapter adapter = new Polar2DAdapter(new Vector2D(3, 4));

        assertEquals(5.0, adapter.abs(), DELTA);
        assertArrayEquals(new double[]{3, 4}, adapter.getComponents());
    }
}

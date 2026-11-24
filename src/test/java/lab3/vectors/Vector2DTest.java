package lab3.vectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Vector2DTest {
    private static final double DELTA = 1e-10;

    @Test
    void absForThreeFourIsFive() {
        assertEquals(5.0, new Vector2D(3, 4).abs(), DELTA);
    }

    @Test
    void absForZeroVectorIsZero() {
        assertEquals(0.0, new Vector2D(0, 0).abs(), DELTA);
    }

    @Test
    void getComponentsReturnsVectorComponents() {
        assertArrayEquals(new double[]{3, 4}, new Vector2D(3, 4).getComponents());
    }

    @Test
    void cdotReturnsElevenForThreeFourAndOneTwo() {
        assertEquals(11.0, new Vector2D(3, 4).cdot(new Vector2D(1, 2)), DELTA);
    }

    @Test
    void cdotReturnsFourteenForThreeFourAndMinusTwoFive() {
        assertEquals(14.0, new Vector2D(3, 4).cdot(new Vector2D(-2, 5)), DELTA);
    }

    @Test
    void cdotOfPerpendicularVectorsIsZero() {
        assertEquals(0.0, new Vector2D(1, 0).cdot(new Vector2D(0, 1)), DELTA);
    }
}

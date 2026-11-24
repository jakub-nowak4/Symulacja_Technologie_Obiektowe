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

    @Test
    void getXAndGetYReturnComponents() {
        Vector2D v = new Vector2D(3, -4);

        assertEquals(3.0, v.getX(), DELTA);
        assertEquals(-4.0, v.getY(), DELTA);
    }

    @Test
    void plusAddsComponents() {
        Vector2D result = new Vector2D(1, 2).plus(new Vector2D(3, -5));

        assertArrayEquals(new double[]{4, -3}, result.getComponents());
    }

    @Test
    void minusSubtractsComponents() {
        Vector2D result = new Vector2D(4, 1).minus(new Vector2D(1, 3));

        assertArrayEquals(new double[]{3, -2}, result.getComponents());
    }

    @Test
    void timesScalesComponents() {
        assertArrayEquals(new double[]{3, -6}, new Vector2D(1, -2).times(3).getComponents());
    }

    @Test
    void timesZeroGivesZeroVector() {
        assertEquals(0.0, new Vector2D(3, 4).times(0).abs(), DELTA);
    }

    @Test
    void timesNegativeScalarReversesVector() {
        assertArrayEquals(new double[]{-3, -4}, new Vector2D(3, 4).times(-1).getComponents());
    }

    @Test
    void operationsDoNotModifyOperands() {
        Vector2D a = new Vector2D(1, 2);
        Vector2D b = new Vector2D(3, 4);

        a.plus(b);
        a.minus(b);
        a.times(5);

        assertArrayEquals(new double[]{1, 2}, a.getComponents());
        assertArrayEquals(new double[]{3, 4}, b.getComponents());
    }

    @Test
    void squaredDistanceViaMinusAndCdot() {
        Vector2D d = new Vector2D(1, 1).minus(new Vector2D(4, 5));

        assertEquals(25.0, d.cdot(d), DELTA);
    }

    @Test
    void fromPolarWithZeroAngleLiesOnPositiveXAxis() {
        Vector2D v = Vector2D.fromPolar(2.5, 0);

        assertEquals(2.5, v.getX(), DELTA);
        assertEquals(0.0, v.getY(), DELTA);
    }

    @Test
    void fromPolarWithRightAngleLiesOnPositiveYAxis() {
        Vector2D v = Vector2D.fromPolar(2.0, Math.PI / 2);

        assertEquals(0.0, v.getX(), DELTA);
        assertEquals(2.0, v.getY(), DELTA);
    }

    @Test
    void fromPolarWithZeroLengthIsZeroVector() {
        assertEquals(0.0, Vector2D.fromPolar(0, 1.234).abs(), DELTA);
    }

    @Test
    void fromPolarRoundTripsThroughAbsAndAdapterAngle() {
        Vector2D v = Vector2D.fromPolar(1.7, -2.1);

        assertEquals(1.7, v.abs(), DELTA);
        assertEquals(-2.1, new Polar2DAdapter(v).getAngle(), DELTA);
    }
}

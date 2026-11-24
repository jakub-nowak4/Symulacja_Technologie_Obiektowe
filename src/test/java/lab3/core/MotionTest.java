package lab3.core;

import lab3.vectors.Vector2D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MotionTest {
    private static final double DELTA = 1e-10;

    @Test
    void speedIsLengthOfVelocity() {
        Motion m = new Motion(new Vector2D(0, 0), new Vector2D(3, 4));

        assertEquals(5.0, m.speed(), DELTA);
    }

    @Test
    void headingIsAngleOfVelocity() {
        Motion m = new Motion(new Vector2D(5, 5), new Vector2D(0, 2));

        assertEquals(Math.PI / 2, m.heading(), DELTA);
    }

    @Test
    void withPositionKeepsVelocity() {
        Vector2D velocity = new Vector2D(1, 2);
        Motion m = new Motion(new Vector2D(0, 0), velocity);

        Motion moved = m.withPosition(new Vector2D(7, 8));

        assertEquals(7.0, moved.position().getX(), DELTA);
        assertEquals(8.0, moved.position().getY(), DELTA);
        assertSame(velocity, moved.velocity());
    }

    @Test
    void withVelocityKeepsPosition() {
        Vector2D position = new Vector2D(1, 2);
        Motion m = new Motion(position, new Vector2D(0, 0));

        Motion turned = m.withVelocity(new Vector2D(-1, 0));

        assertSame(position, turned.position());
        assertEquals(-1.0, turned.velocity().getX(), DELTA);
    }

    @Test
    void withMethodsDoNotChangeTheOriginal() {
        Motion m = new Motion(new Vector2D(1, 1), new Vector2D(2, 2));

        m.withPosition(new Vector2D(9, 9));
        m.withVelocity(new Vector2D(9, 9));

        assertEquals(1.0, m.position().getX(), DELTA);
        assertEquals(2.0, m.velocity().getX(), DELTA);
    }

    @Test
    void rejectsNullComponents() {
        assertThrows(IllegalArgumentException.class, () -> new Motion(null, new Vector2D(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> new Motion(new Vector2D(0, 0), null));
    }
}

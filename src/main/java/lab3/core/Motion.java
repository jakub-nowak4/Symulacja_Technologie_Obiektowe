package lab3.core;

import lab3.vectors.Polar2DAdapter;
import lab3.vectors.Vector2D;


public record Motion(Vector2D position, Vector2D velocity) {

    public Motion {
        if (position == null || velocity == null) {
            throw new IllegalArgumentException("position and velocity must not be null");
        }
    }

    public double speed() {
        return velocity.abs();
    }

    public double heading() {
        return new Polar2DAdapter(velocity).getAngle();
    }

    public Motion withPosition(Vector2D newPosition) {
        return new Motion(newPosition, velocity);
    }

    public Motion withVelocity(Vector2D newVelocity) {
        return new Motion(position, newVelocity);
    }
}

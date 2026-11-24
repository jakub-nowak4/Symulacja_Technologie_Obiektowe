package lab3.core;

import lab3.vectors.Vector2D;

/**
 * Rectangular area {@code [0, width] x [0, height]} in metres. The border belongs to the area.
 * Pure geometry: it knows nothing about people or health.
 */
public record Area(double width, double height) {

    public Area {
        if (!(width > 0) || Double.isInfinite(width) || !(height > 0) || Double.isInfinite(height)) {
            throw new IllegalArgumentException("width and height must be positive and finite: " + width + " x " + height);
        }
    }

    public double perimeter() {
        return 2 * (width + height);
    }

    public boolean contains(Vector2D p) {
        return p.getX() >= 0 && p.getX() <= width && p.getY() >= 0 && p.getY() <= height;
    }

    /**
     * Returns a point on the border, uniformly distributed along the perimeter (so every side is
     * chosen with probability proportional to its length). Uses exactly one draw.
     */
    public Vector2D randomBoundaryPoint(Rng rng) {
        double t = rng.range(0, perimeter());
        if (t < width) {                       // bottom side, left to right
            return new Vector2D(t, 0);
        }
        t -= width;
        if (t < height) {                      // right side, bottom to top
            return new Vector2D(width, t);
        }
        t -= height;
        if (t < width) {                       // top side, right to left
            return new Vector2D(Math.max(0, width - t), height);
        }
        t -= width;                            // left side, top to bottom
        return new Vector2D(0, Math.max(0, height - t));
    }

    /** Returns a point uniformly distributed over the whole area. Uses exactly two draws (x, then y). */
    public Vector2D randomInteriorPoint(Rng rng) {
        double x = rng.range(0, width);
        double y = rng.range(0, height);
        return new Vector2D(x, y);
    }

    /**
     * Returns the velocity with every component that points out of the area flipped, so that a
     * person standing at the given border point moves into the area. At a corner both axes are
     * checked. Components along the border, or already pointing inwards, stay unchanged.
     *
     * @throws IllegalArgumentException if the point does not lie on the border
     */
    public Vector2D directInward(Vector2D borderPoint, Vector2D velocity) {
        double x = borderPoint.getX();
        double y = borderPoint.getY();
        if (!contains(borderPoint) || (x != 0 && x != width && y != 0 && y != height)) {
            throw new IllegalArgumentException("point is not on the border: (" + x + ", " + y + ")");
        }
        double vx = velocity.getX();
        double vy = velocity.getY();
        if ((x == 0 && vx < 0) || (x == width && vx > 0)) {
            vx = -vx;
        }
        if ((y == 0 && vy < 0) || (y == height && vy > 0)) {
            vy = -vy;
        }
        return new Vector2D(vx, vy);
    }
}

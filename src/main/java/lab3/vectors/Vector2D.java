package lab3.vectors;

public class Vector2D implements Vector{
    private final double x;
    private final double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Creates a vector from polar coordinates.
     *
     * @param r     length of the vector
     * @param angle angle in radians, measured from the positive X axis
     */
    public static Vector2D fromPolar(double r, double angle) {
        return new Vector2D(r * Math.cos(angle), r * Math.sin(angle));
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Vector2D plus(Vector2D other) {
        return new Vector2D(x + other.x, y + other.y);
    }

    public Vector2D minus(Vector2D other) {
        return new Vector2D(x - other.x, y - other.y);
    }

    public Vector2D times(double scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }

    @Override
    public double abs() {
        return Math.sqrt(x * x + y * y);
    }

    @Override
    public double cdot(Vector param) {
        return this.x * param.getComponents()[0] + this.y * param.getComponents()[1];
    }

    @Override
    public double[] getComponents() {
        return new double[]{x, y};
    }
}

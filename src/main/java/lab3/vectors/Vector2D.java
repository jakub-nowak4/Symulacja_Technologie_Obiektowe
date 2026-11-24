package lab3.vectors;

public class Vector2D implements Vector{
    private double x;
    private double y;

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
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

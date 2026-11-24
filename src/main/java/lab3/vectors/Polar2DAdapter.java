package lab3.vectors;

public class Polar2DAdapter implements Vector, Polar2D{
    private Vector v;

    public Polar2DAdapter(Vector v) {
        this.v = v;
    }

    @Override
    public double getAngle() {
        return Math.atan2(v.getComponents()[1], v.getComponents()[0]);
    }

    @Override
    public double abs() {
        return v.abs();
    }

    @Override
    public double cdot(Vector param) {
        return v.cdot(param);
    }

    @Override
    public double[] getComponents() {
        return v.getComponents();
    }
}

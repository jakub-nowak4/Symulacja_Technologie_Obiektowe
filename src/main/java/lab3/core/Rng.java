package lab3.core;


public interface Rng {

    /** Returns a uniformly distributed value in {@code [0, 1)}. */
    double nextDouble();


    long state();

    void setState(long state);

    default int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive: " + bound);
        }
        return (int) (nextDouble() * bound);
    }


    default boolean chance(double p) {
        if (Double.isNaN(p) || p < 0 || p > 1) {
            throw new IllegalArgumentException("probability must be in [0, 1]: " + p);
        }
        return nextDouble() < p;
    }


    default double range(double lo, double hi) {
        if (Double.isNaN(lo) || Double.isNaN(hi) || lo > hi) {
            throw new IllegalArgumentException("invalid range: [" + lo + ", " + hi + ")");
        }
        return lo + nextDouble() * (hi - lo);
    }
}

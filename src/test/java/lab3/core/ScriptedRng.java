package lab3.core;

/**
 * Test helper: an {@link Rng} that returns prepared values, so that probability thresholds
 * (for example 0.49 versus 0.50) can be tested deterministically.
 */
public final class ScriptedRng implements Rng {
    private final double[] values;
    private final boolean repeatLast;
    private long state;

    /** Returns the given values in order; fails when they run out. */
    public ScriptedRng(double... values) {
        this(false, values);
    }

    /** Returns the given values in order and then keeps returning the last one. */
    public static ScriptedRng repeatingLast(double... values) {
        return new ScriptedRng(true, values);
    }

    private ScriptedRng(boolean repeatLast, double... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException("at least one value is required");
        }
        for (double v : values) {
            if (!(v >= 0 && v < 1)) {
                throw new IllegalArgumentException("values must be in [0, 1): " + v);
            }
        }
        this.values = values.clone();
        this.repeatLast = repeatLast;
    }

    @Override
    public double nextDouble() {
        if (state >= values.length) {
            if (!repeatLast) {
                throw new IllegalStateException("ScriptedRng exhausted after " + values.length + " values");
            }
            state++;
            return values[values.length - 1];
        }
        return values[(int) state++];
    }

    @Override
    public long state() {
        return state;
    }

    @Override
    public void setState(long state) {
        this.state = state;
    }
}

package lab3.core;


public final class Ticks {
    public static final int PER_SECOND = 25;

    public static final double DT = 1.0 / PER_SECOND;

    private Ticks() {
    }


    public static long fromSeconds(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("seconds must be a finite, non-negative number: " + seconds);
        }
        return Math.round(seconds * PER_SECOND);
    }

    public static double toSeconds(long ticks) {
        return (double) ticks / PER_SECOND;
    }
}

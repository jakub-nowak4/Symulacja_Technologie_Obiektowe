package lab3.core;


public final class SplitMix64Rng implements Rng {
    private static final long GAMMA = 0x9E3779B97F4A7C15L;

    private long state;

    public SplitMix64Rng(long seed) {
        this.state = seed;
    }

    public long nextLong() {
        state += GAMMA;
        long z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    @Override
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
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

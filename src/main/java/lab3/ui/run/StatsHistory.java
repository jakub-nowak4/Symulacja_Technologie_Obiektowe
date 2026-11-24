package lab3.ui.run;

import lab3.core.Ticks;
import lab3.core.view.Stats;
import lab3.core.view.WorldView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The numbers behind the chart: how many people were in each health category, once per simulated
 * second. It is fed with the views that the window shows, so it is only as detailed as the
 * window: at high playback speed a frame may jump over several seconds, and then there is one
 * sample for the whole jump.
 *
 * <p>Rules:
 * <ul>
 *   <li>The first view gives the first sample; a later one gives a new sample when at least
 *       {@link #SAMPLE_TICKS} ticks have passed since the last sample.</li>
 *   <li>If time goes back (a memento was restored), the samples that lie in the future of the new
 *       moment are dropped, so the chart always shows the way to the present moment.</li>
 *   <li>{@link #restart} forgets everything and starts again from one view; it is for a memento
 *       that may come from another run, when the old samples would not lead to it.</li>
 *   <li>At most {@link #MAX_SAMPLES} samples are kept; the oldest are dropped first.</li>
 * </ul>
 */
public final class StatsHistory {
    /** One sample per simulated second. */
    public static final int SAMPLE_TICKS = Ticks.PER_SECOND;
    /** Fifteen minutes of simulated time. */
    public static final int MAX_SAMPLES = 900;

    /** The counts at one moment. */
    public record Sample(long tick, Stats stats) {
        public Sample {
            if (stats == null) {
                throw new IllegalArgumentException("stats must not be null");
            }
        }

        public double seconds() {
            return Ticks.toSeconds(tick);
        }
    }

    private final int capacity;
    private final List<Sample> samples = new ArrayList<>();
    private int version;

    public StatsHistory() {
        this(MAX_SAMPLES);
    }

    StatsHistory(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1: " + capacity);
        }
        this.capacity = capacity;
    }

    /** Takes note of the world as the window now shows it. */
    public void update(WorldView view) {
        requireView(view);
        long tick = view.tick();
        boolean changed = false;
        while (!samples.isEmpty() && last().tick() > tick) {
            samples.remove(samples.size() - 1);
            changed = true;
        }
        if (samples.isEmpty() || tick - last().tick() >= SAMPLE_TICKS) {
            samples.add(new Sample(tick, view.stats()));
            if (samples.size() > capacity) {
                samples.remove(0);
            }
            changed = true;
        }
        if (changed) {
            version++;
        }
    }

    /** Forgets all samples and starts again from this view. */
    public void restart(WorldView view) {
        requireView(view);
        samples.clear();
        samples.add(new Sample(view.tick(), view.stats()));
        version++;
    }

    /** The samples, from the oldest; a copy. */
    public List<Sample> samples() {
        return List.copyOf(samples);
    }

    /** The sample with the most infected people (the earliest one if several are equal), if there is any sample. */
    public Optional<Sample> peakInfected() {
        Sample peak = null;
        for (Sample sample : samples) {
            if (peak == null || sample.stats().infected() > peak.stats().infected()) {
                peak = sample;
            }
        }
        return Optional.ofNullable(peak);
    }

    /** Grows every time the samples change, so that a window can redraw the chart only when needed. */
    public int version() {
        return version;
    }

    private Sample last() {
        return samples.get(samples.size() - 1);
    }

    private static void requireView(WorldView view) {
        if (view == null) {
            throw new IllegalArgumentException("view must not be null");
        }
    }
}

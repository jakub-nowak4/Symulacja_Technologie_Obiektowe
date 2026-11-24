package lab3.ui.run;

import lab3.core.Ticks;

import java.util.List;
import java.util.Locale;

/**
 * Decides how many simulation steps to run in a frame of the window, from the real time that has
 * passed since the previous frame. At speed 1 the simulation runs in real time (25 steps per
 * second); at speed 5 it runs five times faster.
 *
 * <p>Nothing is run while paused. A long gap between frames (the window was blocked) is cut short,
 * so the simulation does not rush to catch up. Contains no JavaFX, so it can be tested without a
 * window.
 */
public final class Playback {
    /** Playback speeds offered to the user. */
    public static final List<Double> SPEEDS = List.of(0.25, 0.5, 1.0, 2.0, 5.0, 10.0, 25.0);

    static final double MAX_FRAME_SECONDS = 0.25;
    static final int MAX_STEPS_PER_FRAME = 500;

    private boolean running;
    private double speed = 1.0;
    private double pendingSteps;

    public boolean isRunning() {
        return running;
    }

    /** Starts running; time that passed before this call is not counted. */
    public void start() {
        running = true;
    }

    /** Stops running; a part of a step that was not yet due is dropped. */
    public void pause() {
        running = false;
        pendingSteps = 0;
    }

    public double speed() {
        return speed;
    }

    /** Sets how many times faster than real time the simulation runs; must be positive. */
    public void setSpeed(double speed) {
        if (!(speed > 0) || Double.isInfinite(speed)) {
            throw new IllegalArgumentException("speed must be positive and finite: " + speed);
        }
        this.speed = speed;
    }

    /**
     * Returns the number of steps to run now.
     *
     * @param elapsedNanos real time since the previous frame, in nanoseconds
     */
    public int stepsFor(long elapsedNanos) {
        if (!running || elapsedNanos <= 0) {
            return 0;
        }
        double seconds = Math.min(elapsedNanos / 1e9, MAX_FRAME_SECONDS);
        pendingSteps += seconds * Ticks.PER_SECOND * speed;
        int steps = (int) pendingSteps;
        pendingSteps -= steps;
        if (steps > MAX_STEPS_PER_FRAME) {
            steps = MAX_STEPS_PER_FRAME;
            pendingSteps = 0;
        }
        return steps;
    }

    /** Text for a speed, for example {@code 0.25×} or {@code 5×}. */
    public static String label(double speed) {
        if (speed == Math.rint(speed)) {
            return String.format(Locale.ROOT, "%d×", (long) speed);
        }
        return String.format(Locale.ROOT, "%s×", Double.toString(speed));
    }
}

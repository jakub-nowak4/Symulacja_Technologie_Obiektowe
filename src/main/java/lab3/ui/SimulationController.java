package lab3.ui;

import javafx.animation.AnimationTimer;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import lab3.core.Simulation;
import lab3.core.SimulationEngine;
import lab3.core.view.WorldView;
import lab3.ui.run.Playback;
import lab3.ui.run.RunSettings;

import java.util.function.Consumer;

/**
 * Connects the window to the simulation: runs the steps in time with the screen, and handles
 * start, pause, a single step, speed and a new simulation. The decisions about <em>how many</em>
 * steps to run live in {@link Playback}; this class only calls them from the JavaFX timer, so it
 * must be used on the JavaFX thread.
 *
 * <p>Every change of the world is passed to the listener as an immutable {@link WorldView}.
 */
public final class SimulationController {
    private final Playback playback = new Playback();
    private final Consumer<WorldView> viewListener;
    private final ReadOnlyBooleanWrapper running = new ReadOnlyBooleanWrapper(false);
    private final AnimationTimer timer;

    private SimulationEngine engine;
    private RunSettings settings;
    private long lastFrameNanos;

    /** Creates a controller with a new simulation made from the given settings; shows it at once. */
    public SimulationController(RunSettings settings, Consumer<WorldView> viewListener) {
        this.viewListener = viewListener;
        this.timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                onFrame(now);
            }
        };
        newSimulation(settings);
    }

    /** Throws away the current simulation and starts a new, paused one at time zero. */
    public void newSimulation(RunSettings newSettings) {
        pause();
        this.settings = newSettings;
        this.engine = Simulation.withSeed(newSettings.config(), newSettings.seed());
        publish();
    }

    public void start() {
        if (playback.isRunning()) {
            return;
        }
        playback.start();
        lastFrameNanos = 0;
        running.set(true);
        timer.start();
    }

    public void pause() {
        playback.pause();
        timer.stop();
        running.set(false);
    }

    /** Runs exactly one step; only when paused, so the picture does not jump during playback. */
    public void stepOnce() {
        if (playback.isRunning()) {
            return;
        }
        engine.step();
        publish();
    }

    public void setSpeed(double speed) {
        playback.setSpeed(speed);
    }

    public double speed() {
        return playback.speed();
    }

    public ReadOnlyBooleanProperty runningProperty() {
        return running.getReadOnlyProperty();
    }

    /** The settings of the simulation that is running now. */
    public RunSettings settings() {
        return settings;
    }

    /** The simulation itself, for the parts of the window that save and restore it. */
    SimulationEngine engine() {
        return engine;
    }

    /** Draws the current state again, for example after a memento has been restored. */
    void publish() {
        viewListener.accept(engine.view());
    }

    private void onFrame(long now) {
        if (lastFrameNanos == 0) {
            lastFrameNanos = now;
            return;
        }
        int steps = playback.stepsFor(now - lastFrameNanos);
        lastFrameNanos = now;
        if (steps > 0) {
            for (int i = 0; i < steps; i++) {
                engine.step();
            }
            publish();
        }
    }
}

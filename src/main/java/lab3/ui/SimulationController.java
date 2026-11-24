package lab3.ui;

import javafx.animation.AnimationTimer;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import lab3.core.Simulation;
import lab3.core.SimulationEngine;
import lab3.core.memento.SimulationMemento;
import lab3.core.view.WorldView;
import lab3.ui.run.Playback;
import lab3.ui.run.RunSettings;
import lab3.ui.run.Snapshots;

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
    private Snapshots snapshots;
    private RunSettings settings;
    private boolean autoSnapshots;
    private Runnable snapshotListener = () -> { };
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
        this.snapshots = new Snapshots(engine);
        snapshots.setAutoEnabled(autoSnapshots);
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
        snapshots.afterStep();
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

    /** The mementos of the simulation that is running now; a new simulation has its own, empty ones. */
    public Snapshots snapshots() {
        return snapshots;
    }

    /** Takes a memento of the simulation as it is now. */
    public Snapshots.Item takeSnapshot() {
        Snapshots.Item item = snapshots.takeManual();
        publish();
        return item;
    }

    /** Keeps a memento read from a file (it is not restored until the user asks). */
    public Snapshots.Item addSnapshot(SimulationMemento memento) {
        Snapshots.Item item = snapshots.addManual(memento);
        publish();
        return item;
    }

    /** Sends the simulation back to the moment of the memento and draws it. */
    public void restoreSnapshot(Snapshots.Item item) {
        snapshots.restore(item);
        publish();
    }

    /** Whether a memento is taken at every whole second of simulated time; it stays so for new simulations. */
    public void setAutoSnapshots(boolean enabled) {
        autoSnapshots = enabled;
        snapshots.setAutoEnabled(enabled);
        publish();
    }

    public boolean autoSnapshots() {
        return autoSnapshots;
    }

    /** Called after every redraw, so that the list of mementos can follow the simulation. */
    public void setSnapshotListener(Runnable listener) {
        this.snapshotListener = listener;
        listener.run();
    }

    private void publish() {
        viewListener.accept(engine.view());
        snapshotListener.run();
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
                snapshots.afterStep();
            }
            publish();
        }
    }
}

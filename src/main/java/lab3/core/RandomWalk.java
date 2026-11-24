package lab3.core;

import lab3.vectors.Vector2D;

/**
 * Random movement of a single person: occasional random changes of speed and heading,
 * followed by a straight step of one tick.
 *
 * <p>Draws from the {@link Rng} in a fixed order: one draw to decide whether the velocity
 * changes, and only if it does, one draw for the heading change and one for the speed change.
 */
public final class RandomWalk {

    private RandomWalk() {
    }

    /** Returns the motion after one tick. Does not handle the area border. */
    public static Motion next(Motion motion, Rng rng, SimulationConfig config) {
        Motion current = motion;
        if (rng.chance(config.velocityChangeProbability())) {
            current = changeVelocity(motion, rng, config);
        }
        Vector2D step = current.velocity().times(Ticks.DT);
        return current.withPosition(current.position().plus(step));
    }

    private static Motion changeVelocity(Motion motion, Rng rng, SimulationConfig config) {
        double turn = rng.range(-config.maxTurnAngle(), config.maxTurnAngle());
        double speedDelta = rng.range(-config.maxSpeedDelta(), config.maxSpeedDelta());

        double heading = motion.heading() + turn;
        double speed = clamp(motion.speed() + speedDelta, config.minSpeed(), SimulationConfig.MAX_SPEED);

        return motion.withVelocity(Vector2D.fromPolar(speed, heading));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

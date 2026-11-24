package lab3.core;

import lab3.vectors.Vector2D;

/**
 * What happens when a person reaches the border of the area: with probability
 * {@link SimulationConfig#BOUNCE_PROBABILITY} it turns back, otherwise it leaves.
 *
 * <p>The decision is made once, when the position is strictly outside the area <em>and</em> the
 * velocity points outwards on that axis. A person standing exactly on the border, or moving along
 * it, is not affected. A bounce flips the outward velocity components and clamps the position to
 * the border, so the same person cannot trigger the decision again in the next tick. Uses one draw
 * when the decision is made and none otherwise.
 */
public final class BoundaryPolicy {
    private final Area area;

    public BoundaryPolicy(Area area) {
        this.area = area;
    }

    public BoundaryResult apply(Motion motion, Rng rng) {
        double x = motion.position().getX();
        double y = motion.position().getY();
        double vx = motion.velocity().getX();
        double vy = motion.velocity().getY();

        boolean crossedX = (x < 0 && vx < 0) || (x > area.width() && vx > 0);
        boolean crossedY = (y < 0 && vy < 0) || (y > area.height() && vy > 0);
        if (!crossedX && !crossedY) {
            return new BoundaryResult(BoundaryResult.Outcome.INSIDE, motion);
        }

        if (!rng.chance(SimulationConfig.BOUNCE_PROBABILITY)) {
            return new BoundaryResult(BoundaryResult.Outcome.EXITED, motion);
        }

        double newX = crossedX ? clamp(x, area.width()) : x;
        double newY = crossedY ? clamp(y, area.height()) : y;
        double newVx = crossedX ? -vx : vx;
        double newVy = crossedY ? -vy : vy;
        Motion bounced = new Motion(new Vector2D(newX, newY), new Vector2D(newVx, newVy));
        return new BoundaryResult(BoundaryResult.Outcome.BOUNCED, bounced);
    }

    private static double clamp(double value, double max) {
        return Math.max(0, Math.min(max, value));
    }
}

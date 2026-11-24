package lab3.core;

/**
 * Outcome of the border check for one person.
 *
 * @param outcome what happened
 * @param motion  the motion to continue with; unchanged unless the person bounced
 */
public record BoundaryResult(Outcome outcome, Motion motion) {

    public enum Outcome {
        /** The person did not reach the border (or is not moving out of the area). */
        INSIDE,
        /** The person turned back: velocity flipped on the crossed axes, position clamped to the border. */
        BOUNCED,
        /** The person left the area and should be removed. */
        EXITED
    }

    public BoundaryResult {
        if (outcome == null || motion == null) {
            throw new IllegalArgumentException("outcome and motion must not be null");
        }
    }
}

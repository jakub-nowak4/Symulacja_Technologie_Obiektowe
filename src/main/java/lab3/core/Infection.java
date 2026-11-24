package lab3.core;

/** A transmission decided by the {@link ContactTracker}: {@code sourceId} infects {@code targetId}. */
public record Infection(int targetId, int sourceId) {
}

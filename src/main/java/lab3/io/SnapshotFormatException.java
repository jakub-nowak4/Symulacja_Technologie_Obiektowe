package lab3.io;

import java.io.IOException;

/** The file is not a memento written by {@link SnapshotStore}, or it is damaged. */
public final class SnapshotFormatException extends IOException {
    private static final long serialVersionUID = 1L;

    public SnapshotFormatException(String message) {
        super(message);
    }

    public SnapshotFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

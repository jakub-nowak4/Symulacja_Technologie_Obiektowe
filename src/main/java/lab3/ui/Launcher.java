package lab3.ui;

import javafx.application.Application;

/**
 * Starts the application. It is a separate class that does not extend {@link Application}, because a
 * main class that extends it often fails to start from the class path ("JavaFX runtime components are
 * missing"). Run it with {@code mvn javafx:run} or from the IDE.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}

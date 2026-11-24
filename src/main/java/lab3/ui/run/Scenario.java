package lab3.ui.run;

/** The two scenarios of the assignment. */
public enum Scenario {
    NO_IMMUNITY("Scenariusz 1: bez odporności"),
    WITH_IMMUNITY("Scenariusz 2: z odpornością");

    private final String title;

    Scenario(String title) {
        this.title = title;
    }

    /** Name shown to the user. */
    public String title() {
        return title;
    }
}

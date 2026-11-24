package lab3.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lab3.ui.run.Playback;
import lab3.ui.run.RunSettings;
import lab3.ui.run.Scenario;

/**
 * Buttons and fields that control the simulation: start/pause, one step, speed, and the choice of
 * scenario, immunity and seed for a new simulation.
 */
public final class ControlPanel extends VBox {
    private final SimulationController controller;
    private final Button startPause = new Button("Start");
    private final Button step = new Button("Krok");
    private final ComboBox<Double> speed = new ComboBox<>();
    private final ComboBox<Scenario> scenario = new ComboBox<>();
    private final Slider immunity = new Slider(0, 100, RunSettings.DEFAULT_IMMUNITY * 100);
    private final Label immunityLabel = new Label();
    private final TextField seed = new TextField(Long.toString(RunSettings.DEFAULT_SEED));
    private final Label error = new Label();

    public ControlPanel(SimulationController controller) {
        super(10);
        this.controller = controller;
        setPadding(new Insets(12));

        startPause.setPrefWidth(110);
        startPause.setOnAction(e -> {
            if (controller.runningProperty().get()) {
                controller.pause();
            } else {
                controller.start();
            }
        });
        step.setOnAction(e -> controller.stepOnce());
        controller.runningProperty().addListener((o, was, is) -> showRunning(is));
        showRunning(controller.runningProperty().get());

        speed.getItems().addAll(Playback.SPEEDS);
        speed.setValue(controller.speed());
        speed.setConverter(new StringConverter<>() {
            @Override
            public String toString(Double value) {
                return value == null ? "" : Playback.label(value);
            }

            @Override
            public Double fromString(String text) {
                return null;
            }
        });
        speed.valueProperty().addListener((o, was, is) -> {
            if (is != null) {
                controller.setSpeed(is);
            }
        });

        scenario.getItems().addAll(Scenario.values());
        scenario.setValue(controller.settings().scenario());
        scenario.setConverter(new StringConverter<>() {
            @Override
            public String toString(Scenario value) {
                return value == null ? "" : value.title();
            }

            @Override
            public Scenario fromString(String text) {
                return null;
            }
        });
        scenario.setMaxWidth(Double.MAX_VALUE);
        scenario.valueProperty().addListener((o, was, is) -> showImmunityEnabled());

        immunity.valueProperty().addListener((o, was, is) -> showImmunity());
        showImmunity();
        showImmunityEnabled();

        seed.setPromptText("ziarno (liczba całkowita)");
        error.setWrapText(true);
        error.setStyle("-fx-text-fill: #b00020;");

        Button restart = new Button("Nowa symulacja");
        restart.setMaxWidth(Double.MAX_VALUE);
        restart.setOnAction(e -> restart());

        getChildren().addAll(
                new HBox(8, startPause, step),
                new HBox(8, new Label("Prędkość:"), speed),
                new Separator(),
                new Label("Nowa symulacja"),
                scenario,
                immunityLabel,
                immunity,
                new Label("Ziarno:"),
                seed,
                restart,
                error);
    }

    private void restart() {
        long seedValue;
        try {
            seedValue = RunSettings.parseSeed(seed.getText());
        } catch (IllegalArgumentException e) {
            error.setText(e.getMessage());
            return;
        }
        error.setText("");
        controller.newSimulation(new RunSettings(scenario.getValue(), immunity.getValue() / 100.0, seedValue));
    }

    private void showRunning(boolean running) {
        startPause.setText(running ? "Pauza" : "Start");
        step.setDisable(running);
    }

    private void showImmunity() {
        immunityLabel.setText(String.format("Odporni (scenariusz 2): %.0f%%", immunity.getValue()));
    }

    private void showImmunityEnabled() {
        boolean enabled = scenario.getValue() == Scenario.WITH_IMMUNITY;
        immunity.setDisable(!enabled);
        immunityLabel.setDisable(!enabled);
    }
}

package lab3.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import lab3.core.Simulation;
import lab3.core.SimulationConfig;
import lab3.core.SimulationEngine;
import lab3.core.health.HealthKind;
import lab3.core.view.WorldView;
import lab3.ui.draw.HealthPalette;

/**
 * The main window. In this stage it only shows the world: the simulation is advanced once, to a
 * moment at which infected people are already present, and drawn. Starting, pausing and the other
 * controls come in the next stages.
 */
public final class App extends Application {
    /** Simulated seconds before the first picture, so that all health categories are visible. */
    private static final int PREVIEW_SECONDS = 60;
    private static final long PREVIEW_SEED = 42;

    @Override
    public void start(Stage stage) {
        SimulationEngine engine = Simulation.withSeed(SimulationConfig.noImmunity(), PREVIEW_SEED);
        for (int i = 0; i < PREVIEW_SECONDS * 25; i++) {
            engine.step();
        }
        WorldView view = engine.view();

        WorldCanvas canvas = new WorldCanvas();
        Pane canvasHolder = new Pane(canvas);
        canvasHolder.widthProperty().addListener((o, oldWidth, width) ->
                canvas.resizeTo(width.doubleValue(), canvasHolder.getHeight()));
        canvasHolder.heightProperty().addListener((o, oldHeight, height) ->
                canvas.resizeTo(canvasHolder.getWidth(), height.doubleValue()));
        canvas.setView(view);

        CheckBox contactRadius = new CheckBox("Pokaż promień kontaktu (2 m)");
        contactRadius.setWrapText(true);
        contactRadius.selectedProperty().addListener((o, was, is) -> canvas.setShowContactRadius(is));

        VBox side = new VBox(10);
        side.getChildren().add(new Label("Legenda"));
        for (HealthKind kind : HealthPalette.legendOrder()) {
            side.getChildren().add(legendRow(kind));
        }
        side.getChildren().add(contactRadius);
        side.setPadding(new Insets(12));
        side.setPrefWidth(270);

        Label status = new Label(String.format("t = %.0f s   osób: %d", view.seconds(), view.persons().size()));
        status.setPadding(new Insets(6, 12, 6, 12));

        BorderPane root = new BorderPane();
        root.setCenter(canvasHolder);
        root.setRight(side);
        root.setBottom(status);

        stage.setTitle("Symulacja zakażenia – Stan i Pamiątka");
        stage.setScene(new Scene(root, 900, 680));
        stage.show();
    }

    private static HBox legendRow(HealthKind kind) {
        Circle dot = new Circle(6, Colors.of(kind));
        return new HBox(8, dot, new Label(HealthPalette.label(kind)));
    }
}

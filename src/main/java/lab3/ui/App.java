package lab3.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import lab3.core.health.HealthKind;
import lab3.ui.draw.HealthPalette;
import lab3.ui.run.RunSettings;

/**
 * The main window: the picture of the world in the middle, on the right two tabs (controls and
 * mementos) above the legend, and the time and the number of people at the bottom.
 */
public final class App extends Application {
    private SimulationController controller;

    @Override
    public void start(Stage stage) {
        WorldCanvas canvas = new WorldCanvas();
        Pane canvasHolder = new Pane(canvas);
        canvasHolder.widthProperty().addListener((o, oldWidth, width) ->
                canvas.resizeTo(width.doubleValue(), canvasHolder.getHeight()));
        canvasHolder.heightProperty().addListener((o, oldHeight, height) ->
                canvas.resizeTo(canvasHolder.getWidth(), height.doubleValue()));

        Label status = new Label();
        status.setPadding(new Insets(6, 12, 6, 12));

        controller = new SimulationController(RunSettings.defaults(), view -> {
            canvas.setView(view);
            status.setText(String.format("t = %.1f s   osób: %d", view.seconds(), view.persons().size()));
        });

        CheckBox contactRadius = new CheckBox("Pokaż promień kontaktu (2 m)");
        contactRadius.setWrapText(true);
        contactRadius.selectedProperty().addListener((o, was, is) -> canvas.setShowContactRadius(is));

        VBox legend = new VBox(10);
        legend.setPadding(new Insets(12));
        legend.getChildren().add(new Label("Legenda"));
        for (HealthKind kind : HealthPalette.legendOrder()) {
            legend.getChildren().add(legendRow(kind));
        }
        legend.getChildren().add(contactRadius);

        TabPane tabs = new TabPane(
                new Tab("Sterowanie", new ControlPanel(controller)),
                new Tab("Pamiątki", new SnapshotPanel(controller)));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        VBox side = new VBox(tabs, new Separator(), legend);
        side.setPrefWidth(290);

        BorderPane root = new BorderPane();
        root.setCenter(canvasHolder);
        root.setRight(side);
        root.setBottom(status);

        stage.setTitle("Symulacja zakażenia – Stan i Pamiątka");
        stage.setScene(new Scene(root, 960, 720));
        stage.setOnHidden(e -> controller.pause());
        stage.show();
    }

    private static HBox legendRow(HealthKind kind) {
        Circle dot = new Circle(6, Colors.of(kind));
        return new HBox(8, dot, new Label(HealthPalette.label(kind)));
    }
}

package lab3.ui;

import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lab3.core.health.HealthKind;
import lab3.ui.draw.HealthPalette;
import lab3.ui.run.StatsHistory;
import lab3.ui.run.StatsHistory.Sample;
import lab3.ui.run.StatsText;

import java.util.ArrayList;
import java.util.List;

/**
 * The chart of the number of people in each health category over simulated time, with the highest
 * number of infected people so far. What is on the chart comes from {@link StatsHistory}; this class
 * only draws it, and only when the history has changed.
 */
public final class StatsPanel extends VBox {
    private final SimulationController controller;
    private final List<HealthKind> kinds = HealthPalette.legendOrder();
    private final List<XYChart.Series<Number, Number>> series = new ArrayList<>();
    private final Label peak = new Label();

    private StatsHistory shown;
    private int shownVersion = -1;

    public StatsPanel(SimulationController controller) {
        super(10);
        this.controller = controller;
        setPadding(new Insets(12));

        NumberAxis time = new NumberAxis();
        time.setLabel("czas symulacji [s]");
        time.setForceZeroInRange(true);
        NumberAxis people = new NumberAxis();
        people.setLabel("liczba osób");
        people.setForceZeroInRange(true);

        LineChart<Number, Number> chart = new LineChart<>(time, people);
        chart.setAnimated(false);
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);          // the legend of the window has the same colours
        chart.setPrefHeight(340);
        StringBuilder colours = new StringBuilder();
        for (int i = 0; i < kinds.size(); i++) {
            XYChart.Series<Number, Number> line = new XYChart.Series<>();
            line.setName(HealthPalette.label(kinds.get(i)));
            series.add(line);
            colours.append("CHART_COLOR_").append(i + 1).append(": ").append(HealthPalette.hex(kinds.get(i))).append(";");
        }
        chart.setStyle(colours.toString());
        chart.getData().addAll(series);
        VBox.setVgrow(chart, Priority.ALWAYS);

        peak.setWrapText(true);
        Label hint = new Label("Jeden punkt na każdą sekundę czasu symulacji.");
        hint.setWrapText(true);

        getChildren().addAll(hint, chart, peak);
        controller.addRedrawListener(this::refresh);
    }

    private void refresh() {
        StatsHistory history = controller.statsHistory();
        if (history == shown && history.version() == shownVersion) {
            return;
        }
        shown = history;
        shownVersion = history.version();

        List<Sample> samples = history.samples();
        for (int i = 0; i < kinds.size(); i++) {
            HealthKind kind = kinds.get(i);
            List<XYChart.Data<Number, Number>> points = new ArrayList<>(samples.size());
            for (Sample sample : samples) {
                points.add(new XYChart.Data<>(sample.seconds(), HealthPalette.count(sample.stats(), kind)));
            }
            series.get(i).getData().setAll(points);
        }
        peak.setText(StatsText.describePeak(history.peakInfected()));
    }
}

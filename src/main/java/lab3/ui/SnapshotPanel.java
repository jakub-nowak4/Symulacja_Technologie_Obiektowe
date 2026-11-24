package lab3.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lab3.core.memento.SimulationMemento;
import lab3.io.SnapshotFormatException;
import lab3.io.SnapshotStore;
import lab3.ui.run.Snapshots;
import lab3.ui.run.Snapshots.Item;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Buttons and the list for the Memento pattern: take a memento, go back to one, save one to a file,
 * read one from a file, and switch on a memento at every second of simulated time. What is kept,
 * and when, is decided by {@link Snapshots}; this class only shows it and asks for files.
 */
public final class SnapshotPanel extends VBox {
    private static final String ERROR_STYLE = "-fx-text-fill: #b00020;";
    private static final String INFO_STYLE = "-fx-text-fill: #333333;";

    private final SimulationController controller;
    private final ListView<Item> list = new ListView<>();
    private final Label message = new Label();

    private Snapshots shown;
    private int shownVersion = -1;
    private File lastDirectory;

    public SnapshotPanel(SimulationController controller) {
        super(10);
        this.controller = controller;
        setPadding(new Insets(12));

        Label hint = new Label("Pamiątka to zapis stanu symulacji w danej chwili. Można do niej wrócić "
                + "i zacząć od tego miejsca jeszcze raz.");
        hint.setWrapText(true);

        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : describe(item));
            }
        });
        list.setPlaceholder(new Label("Brak pamiątek"));
        list.setPrefHeight(170);
        VBox.setVgrow(list, Priority.ALWAYS);

        Button take = new Button("Zapisz pamiątkę");
        take.setMaxWidth(Double.MAX_VALUE);
        take.setOnAction(e -> take());

        Button restore = new Button("Przywróć wybraną");
        restore.setMaxWidth(Double.MAX_VALUE);
        restore.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        restore.setOnAction(e -> restore());

        Button save = new Button("Zapisz do pliku…");
        save.setMaxWidth(Double.MAX_VALUE);
        save.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        save.setOnAction(e -> saveToFile());

        Button load = new Button("Wczytaj z pliku…");
        load.setMaxWidth(Double.MAX_VALUE);
        load.setOnAction(e -> loadFromFile());

        HBox files = new HBox(8, save, load);
        HBox.setHgrow(save, Priority.ALWAYS);
        HBox.setHgrow(load, Priority.ALWAYS);

        CheckBox auto = new CheckBox("Co sekundę czasu symulacji (ostatnie 10 min)");
        auto.setWrapText(true);
        auto.setSelected(controller.autoSnapshots());
        auto.selectedProperty().addListener((o, was, is) -> controller.setAutoSnapshots(is));

        message.setWrapText(true);
        message.setMinHeight(52);   // room for three lines, so the list does not jump when a message appears

        getChildren().addAll(hint, list, take, restore, files, auto, message);

        controller.addRedrawListener(this::refresh);
    }

    static String describe(Item item) {
        return String.format("%s %d – t = %.1f s", item.kind().title(), item.number(), item.seconds());
    }

    private void take() {
        Item item = controller.takeSnapshot();
        list.getSelectionModel().select(item);
        info("Zapisano pamiątkę dla t = " + seconds(item) + " s.");
    }

    private void restore() {
        Item item = list.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        try {
            controller.restoreSnapshot(item);
            info("Przywrócono stan z t = " + seconds(item) + " s.");
        } catch (IllegalArgumentException e) {
            refresh();
            error("Tej pamiątki już nie ma na liście (starsze są usuwane, gdy lista jest pełna).");
        }
    }

    private void saveToFile() {
        Item item = list.getSelectionModel().getSelectedItem();
        if (item == null) {
            return;
        }
        FileChooser chooser = fileChooser("Zapisz pamiątkę do pliku");
        chooser.setInitialFileName(String.format(Locale.ROOT, "pamiatka-%.1fs.txt", item.seconds()));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        lastDirectory = file.getParentFile();
        try {
            SnapshotStore.save(file.toPath(), controller.snapshots().memento(item));
            info("Zapisano plik: " + file.getName());
        } catch (IOException e) {
            error("Nie udało się zapisać pliku: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            refresh();
            error("Tej pamiątki już nie ma na liście (starsze są usuwane, gdy lista jest pełna).");
        }
    }

    private void loadFromFile() {
        File file = fileChooser("Wczytaj pamiątkę z pliku").showOpenDialog(getScene().getWindow());
        if (file == null) {
            return;
        }
        lastDirectory = file.getParentFile();
        try {
            SimulationMemento memento = SnapshotStore.load(file.toPath());
            Item item = controller.addSnapshot(memento);
            list.getSelectionModel().select(item);
            info("Wczytano pamiątkę dla t = " + seconds(item) + " s. Użyj „Przywróć wybraną”.");
        } catch (SnapshotFormatException e) {
            error("To nie jest poprawny plik pamiątki: " + e.getMessage());
        } catch (IOException e) {
            error("Nie udało się odczytać pliku: " + e.getMessage());
        }
    }

    private FileChooser fileChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Pamiątka (*.txt)", "*.txt"),
                new FileChooser.ExtensionFilter("Wszystkie pliki", "*.*"));
        if (lastDirectory != null && lastDirectory.isDirectory()) {
            chooser.setInitialDirectory(lastDirectory);
        }
        return chooser;
    }

    /** Redraws the list when the simulation has changed it; the chosen line stays chosen if it is still there. */
    private void refresh() {
        Snapshots current = controller.snapshots();
        if (current == shown && current.version() == shownVersion) {
            return;
        }
        if (shown != null && current != shown) {
            message.setText("");   // a new simulation: what the last message said is no longer true
        }
        shown = current;
        shownVersion = current.version();
        Item chosen = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(current.items());
        if (chosen != null && list.getItems().contains(chosen)) {
            list.getSelectionModel().select(chosen);
        }
    }

    private static String seconds(Item item) {
        return String.format("%.1f", item.seconds());
    }

    private void info(String text) {
        message.setStyle(INFO_STYLE);
        message.setText(text);
    }

    private void error(String text) {
        message.setStyle(ERROR_STYLE);
        message.setText(text);
    }
}

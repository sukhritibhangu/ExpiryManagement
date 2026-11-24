package gui;

import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface WorkspaceFiles {
    void bindImage(Button button, Window owner, Consumer<Path> selected, Consumer<String> error);
    void export(Window owner, Consumer<Path> selected);

    default void releaseImage(Path path) { }
    default void discardExport(Path path) { }

    static WorkspaceFiles desktop() {
        return new WorkspaceFiles() {
            @Override public void bindImage(Button button, Window owner, Consumer<Path> selected, Consumer<String> error) {
                button.setOnAction(event -> {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Choose product label image");
                    chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images",
                            List.of("*.png", "*.jpg", "*.jpeg", "*.tif", "*.tiff")));
                    var file = chooser.showOpenDialog(owner);
                    if (file != null) selected.accept(file.toPath());
                });
            }

            @Override public void export(Window owner, Consumer<Path> selected) {
                FileChooser chooser = new FileChooser();
                chooser.setTitle("Export inventory");
                chooser.setInitialFileName("inventory-report.csv");
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
                var file = chooser.showSaveDialog(owner);
                if (file != null) selected.accept(file.toPath());
            }
        };
    }
}

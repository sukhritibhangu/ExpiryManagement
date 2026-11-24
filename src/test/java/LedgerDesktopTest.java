import gui.LedgerApp;
import gui.WorkspaceFiles;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.Chart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.stage.Window;
import model.Item;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import service.ExpiryService;
import service.ItemService;
import storage.CsvStorage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfSystemProperty(named = "ledger.fx.test", matches = "true")
class LedgerDesktopTest {
    @TempDir Path temp;
    private Stage stage;
    private LedgerApp app;

    @Test void exercisesNavigationInventoryAndReports() throws Exception {
        var repository = new CsvStorage(temp.resolve("inventory.csv").toString());
        var service = new ItemService(repository, new ExpiryService(), null, false);
        LocalDate now = LocalDate.now();
        service.addItem("Oat milk", "Dairy", 4, now.plusDays(2), "");
        service.addItem("Rice", "Pantry", 12, now.plusDays(65), "");
        service.addItem("Yoghurt", "Dairy", 2, now.minusDays(2), "");
        service.addItem("Coffee", "Pantry", 7, now.plusDays(25), "");

        Platform.startup(() -> Platform.setImplicitExit(false));
        fx(() -> {
            stage = new Stage();
            app = new LedgerApp() {
                @Override protected ItemService openInventory() { return service; }
                @Override protected Path preferencesPath() { return temp.resolve("profile.properties"); }
                @Override protected WorkspaceFiles workspaceFiles() {
                    return new WorkspaceFiles() {
                        @Override public void bindImage(Button button, Window owner, Consumer<Path> selected,
                                                        Consumer<String> error) {
                            button.setOnAction(event -> selected.accept(temp.resolve("missing.png")));
                        }

                        @Override public void export(Window owner, Consumer<Path> selected) {
                            selected.accept(temp.resolve("report.csv"));
                        }
                    };
                }
            };
            app.start(stage);
            return null;
        });
        try {
            await(() -> hasLabel("Inventory health"));
            Thread.sleep(550);
            assertEquals(2, fx(() -> charts().size()));
            screenshot("dashboard.png");

            click("Toggle navigation");
            assertTrue(fx(() -> button("Inventory").getText().isEmpty()));
            click("Toggle navigation");

            click("Inventory");
            fx(() -> {
                TableView<Item> table = table();
                assertEquals(4, table.getItems().size());
                field("Search products or categories").setText("oat");
                assertEquals("Oat milk", table.getItems().get(0).getName());
                button("Clear filters").fire();
                MenuButton categories = menu("Categories");
                ((CheckMenuItem) categories.getItems().get(0)).setSelected(true);
                assertEquals(2, table.getItems().size());
                MenuButton statuses = menu("Expiry status");
                ((CheckMenuItem) statuses.getItems().get(0)).setSelected(true);
                assertEquals("Yoghurt", table.getItems().get(0).getName());
                button("Clear filters").fire();
                table.getSortOrder().setAll(List.of(table.getColumns().get(2)));
                assertEquals(2, table.getItems().get(0).getQuantity());
                return null;
            });
            screenshot("inventory.png");

            click("Add Product");
            fx(() -> {
                button("Save product").fire();
                assertTrue(hasLabel("Product name is required."));
                field("Product name").setText("Green tea");
                field("Product category").setText("Pantry");
                stage.getScene().getRoot().lookupAll(".date-picker").stream().map(DatePicker.class::cast)
                        .filter(picker -> "Required".equals(picker.getPromptText())).findFirst().orElseThrow()
                        .setValue(now.plusDays(10));
                button("Save product").fire();
                return null;
            });
            await(() -> service.getTotalCount() == 5 && hasLabel("Product details"));
            select("Green tea");
            click("Edit");
            fx(() -> {
                field("Product name").setText("Jasmine tea");
                button("Save changes").fire();
                return null;
            });
            await(() -> hasLabel("Product details") && service.searchItems("Jasmine").size() == 1);
            select("Jasmine tea");
            click("Delete");
            click("Cancel");
            assertEquals(5, service.getTotalCount());
            click("Delete");
            click("Confirm delete");
            await(() -> !button("Undo delete").isDisable());
            assertEquals(4, service.getTotalCount());
            click("Undo delete");
            await(() -> service.getTotalCount() == 5 && button("Undo delete").isDisable());

            click("Expiry Tracker");
            assertEquals("Yoghurt", fx(() -> table().getItems().get(0).getName()));
            click("Calendar");
            click("Next month");
            click("Previous month");
            click("Today");
            assertTrue(fx(() -> hasLabel("Expiries on " + now)));

            click("Analytics");
            assertEquals(4, fx(() -> charts().size()));
            click("Export inventory CSV");
            await(() -> Files.exists(temp.resolve("report.csv")));
            assertEquals(5, new CsvStorage(temp.resolve("report.csv").toString()).loadItems().size());

            click("Notifications");
            click("Mark all as read");
            await(() -> button("Mark all as read").isDisable());
            click("Settings");
            fx(() -> {
                CheckBox reduced = (CheckBox) stage.getScene().lookup(".check-box");
                reduced.setSelected(true);
                button("Save preferences").fire();
                return null;
            });
            await(() -> hasLabel("✓ Preferences saved"));
            click("Scan Product");
            click("Choose label image");
            await(() -> hasLabel("Choose an image between 1 byte and 12 MB."));
            click("Enter product manually");
            assertTrue(fx(() -> hasLabel("Product information")));
            click("Dashboard");
            await(() -> hasLabel("Recent activity"));
        } finally {
            fx(() -> {
                app.stop();
                stage.close();
                return null;
            });
            Platform.exit();
        }
    }

    private void screenshot(String name) throws Exception {
        String destination = System.getProperty("ledger.fx.screenshots");
        if (destination == null) return;
        BufferedImage image = fx(() -> {
            WritableImage snapshot = stage.getScene().snapshot(null);
            int width = (int) snapshot.getWidth();
            int height = (int) snapshot.getHeight();
            var output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            var pixels = snapshot.getPixelReader();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) output.setRGB(x, y, pixels.getArgb(x, y));
            }
            return output;
        });
        Path folder = Files.createDirectories(Path.of(destination));
        ImageIO.write(image, "png", folder.resolve(name).toFile());
    }

    private void select(String product) throws Exception {
        fx(() -> {
            var view = table();
            view.getSelectionModel().select(view.getItems().stream().filter(item -> item.getName().equals(product))
                    .findFirst().orElseThrow());
            return null;
        });
    }

    private void click(String text) throws Exception {
        fx(() -> {
            button(text).fire();
            return null;
        });
    }

    private Button button(String text) {
        return nodes(".button", Button.class).stream()
                .filter(button -> text.equals(button.getText()) || text.equals(button.getAccessibleText()))
                .findFirst().orElseThrow(() -> new AssertionError("Missing button " + text));
    }

    private MenuButton menu(String name) {
        return nodes(".menu-button", MenuButton.class).stream().filter(menu -> name.equals(menu.getText()))
                .findFirst().orElseThrow();
    }

    private boolean hasLabel(String value) {
        return nodes(".label", Label.class).stream().anyMatch(label -> value.equals(label.getText()));
    }

    private TextField field(String prompt) {
        return nodes(".text-field", TextField.class).stream().filter(field -> prompt.equals(field.getPromptText()))
                .findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private TableView<Item> table() { return (TableView<Item>) stage.getScene().lookup(".table-view"); }

    private List<Chart> charts() { return nodes(".chart", Chart.class); }

    private <T extends Node> List<T> nodes(String selector, Class<T> type) {
        Scene scene = stage.getScene();
        scene.getRoot().applyCss();
        scene.getRoot().layout();
        return scene.getRoot().lookupAll(selector).stream().filter(type::isInstance).map(type::cast).toList();
    }

    private void await(Callable<Boolean> condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (System.nanoTime() < deadline) {
            if (fx(condition)) return;
            Thread.sleep(35);
        }
        fail("Timed out waiting for the JavaFX view");
    }

    private static <T> T fx(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(12, TimeUnit.SECONDS);
    }
}

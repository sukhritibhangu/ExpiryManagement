package gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import model.ExpiryStatus;
import model.Item;
import service.ExpiryService;
import service.ItemService;
import storage.StorageFactory;
import util.AppLogging;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * JavaFX desktop UI for the ledger (syllabus topics 12 and 13).
 *
 * <p>Run it with Maven so the JavaFX modules are on the module path:</p>
 * <pre>mvn javafx:run</pre>
 *
 * <p>Event-driven parts: button {@code setOnAction} handlers, a live search field
 * listener, table selection and double-click handlers, a keyboard accelerator and
 * dialog button filters.</p>
 */
public class LedgerApp extends Application {

    private final ExpiryService expiryService = new ExpiryService();
    private final ObservableList<Item> rows = FXCollections.observableArrayList();
    private final TableView<Item> table = new TableView<>(rows);
    private final TextField searchField = new TextField();
    private final Label summaryLabel = new Label();

    private ItemService itemService;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        AppLogging.configure();
        itemService = new ItemService(StorageFactory.create(), expiryService);

        BorderPane root = new BorderPane();
        root.setTop(buildToolbar());
        root.setCenter(buildTable());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1080, 620);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                this::openAddDialog);

        stage.setTitle("Expiry Alert Ledger");
        stage.setScene(scene);
        stage.show();

        refreshRows("");

        // Show the startup alert after the window is visible.
        Platform.runLater(() -> {
            if (!itemService.getAlertItems().isEmpty()) {
                showAlerts();
            }
        });
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private HBox buildToolbar() {
        Label title = new Label("Expiry Alert Ledger");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        searchField.setPromptText("Search name or category…");
        searchField.setPrefWidth(240);

        Button alertsButton = new Button("Alerts");
        alertsButton.setOnAction(event -> showAlerts());

        Button refreshButton = new Button("Refresh");
        refreshButton.setOnAction(event -> refreshRows(searchField.getText()));

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> deleteSelected());

        Button addButton = new Button("Add item");
        addButton.setDefaultButton(true);
        addButton.setOnAction(event -> openAddDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8, title, searchField, spacer,
                alertsButton, refreshButton, deleteButton, addButton);
        bar.setPadding(new Insets(12));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private TableView<Item> buildTable() {
        table.getColumns().add(column("ID", 50, item -> String.valueOf(item.getId())));
        table.getColumns().add(column("Name", 220, Item::getName));
        table.getColumns().add(column("Category", 120, Item::getCategory));
        table.getColumns().add(column("Qty", 55, item -> String.valueOf(item.getQuantity())));
        table.getColumns().add(column("Expiry", 110, item -> item.getExpiryDate().toString()));
        table.getColumns().add(column("Days left", 130, item -> expiryService.formatDaysRemaining(
                expiryService.getDaysRemaining(item.getExpiryDate()))));
        table.getColumns().add(column("Status", 120, item ->
                expiryService.calculateStatus(item.getExpiryDate()).getLabel()));
        table.getColumns().add(column("Notes", 240, Item::getNotes));

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No items yet — press \"Add item\" to create one."));
        table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        // Event-driven row styling: expired rows red, expiring-soon rows amber.
        table.setRowFactory(view -> {
            TableRow<Item> row = new TableRow<>();
            row.itemProperty().addListener((observable, oldItem, newItem) -> {
                row.setStyle("");
                if (newItem == null) {
                    return;
                }
                ExpiryStatus status = expiryService.calculateStatus(newItem.getExpiryDate());
                if (status == ExpiryStatus.EXPIRED) {
                    row.setStyle("-fx-background-color: #fdecec;");
                } else if (status == ExpiryStatus.EXPIRING_SOON) {
                    row.setStyle("-fx-background-color: #fff7e0;");
                }
            });
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showDetails(row.getItem());
                }
            });
            return row;
        });

        // Delete key acts on the selected row.
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                deleteSelected();
            }
        });

        return table;
    }

    private HBox buildStatusBar() {
        HBox bar = new HBox(summaryLabel);
        bar.setPadding(new Insets(8, 12, 8, 12));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private TableColumn<Item, String> column(String title, double width, Function<Item, String> value) {
        TableColumn<Item, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        return column;
    }

    // ------------------------------------------------------------------
    // Data actions
    // ------------------------------------------------------------------

    private void refreshRows(String query) {
        List<Item> items = (query == null || query.isBlank())
                ? itemService.getAllItems()
                : itemService.searchItems(query);
        rows.setAll(items);
        updateSummary();
    }

    private void updateSummary() {
        List<Item> all = itemService.getAllItems();
        long expired = all.stream()
                .filter(item -> expiryService.calculateStatus(item.getExpiryDate()) == ExpiryStatus.EXPIRED)
                .count();
        long soon = all.stream()
                .filter(item -> expiryService.calculateStatus(item.getExpiryDate()) == ExpiryStatus.EXPIRING_SOON)
                .count();
        summaryLabel.setText(all.size() + " item(s)  •  " + expired + " expired  •  "
                + soon + " expiring within 7 days");
    }

    private void openAddDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add inventory item");
        dialog.setHeaderText("Enter the details from the label");

        TextField nameField = new TextField();
        TextField categoryField = new TextField();
        TextField quantityField = new TextField("1");
        DatePicker expiryPicker = new DatePicker(LocalDate.now().plusDays(7));
        TextArea notesField = new TextArea();
        notesField.setPrefRowCount(2);
        notesField.setPromptText("Optional notes");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.addRow(0, new Label("Name"), nameField);
        grid.addRow(1, new Label("Category"), categoryField);
        grid.addRow(2, new Label("Quantity"), quantityField);
        grid.addRow(3, new Label("Expiry date"), expiryPicker);
        grid.addRow(4, new Label("Notes"), notesField);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        Draft draft = new Draft();
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                draft.name = requireText(nameField.getText(), "Name");
                draft.category = requireText(categoryField.getText(), "Category");
                draft.quantity = parseQuantity(quantityField.getText());
                draft.expiryDate = requireDate(expiryPicker.getValue());
                draft.notes = notesField.getText() == null ? "" : notesField.getText().trim();
            } catch (IllegalArgumentException ex) {
                showError(ex.getMessage());
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) {
            itemService.addItem(draft.name, draft.category, draft.quantity, draft.expiryDate, draft.notes);
            refreshRows(searchField.getText());
        }
    }

    private void deleteSelected() {
        Item selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Select an item in the table first.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.getName() + "\"?", ButtonType.OK, ButtonType.CANCEL);
        confirmation.setHeaderText(null);
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            itemService.deleteItem(selected.getId());
            refreshRows(searchField.getText());
        }
    }

    private void showAlerts() {
        List<Item> alerts = itemService.getAlertItems();
        if (alerts.isEmpty()) {
            showInfo("No items are expired or expiring within the next 7 days.");
            return;
        }

        StringBuilder message = new StringBuilder();
        for (Item item : alerts) {
            message.append("• ")
                    .append(item.getSummary())
                    .append(" — ")
                    .append(expiryService.formatDaysRemaining(
                            expiryService.getDaysRemaining(item.getExpiryDate())))
                    .append('\n');
        }

        Alert alert = new Alert(Alert.AlertType.WARNING, message.toString(), ButtonType.OK);
        alert.setTitle("Expiry alerts");
        alert.setHeaderText(alerts.size() + " item(s) need attention");
        alert.showAndWait();
    }

    private void showDetails(Item item) {
        String details = "Name: " + item.getName()
                + "\nCategory: " + item.getCategory()
                + "\nQuantity: " + item.getQuantity()
                + "\nExpiry: " + item.getExpiryDate()
                + "\nDays left: " + expiryService.formatDaysRemaining(
                        expiryService.getDaysRemaining(item.getExpiryDate()))
                + "\nStatus: " + expiryService.calculateStatus(item.getExpiryDate()).getLabel()
                + "\nNotes: " + item.getNotes();

        Alert alert = new Alert(Alert.AlertType.INFORMATION, details, ButtonType.OK);
        alert.setTitle("Item details");
        alert.setHeaderText(item.getSummary());
        alert.showAndWait();
    }

    // ------------------------------------------------------------------
    // Validation helpers (dialog button filter)
    // ------------------------------------------------------------------

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
        return value.trim();
    }

    private static int parseQuantity(String value) {
        try {
            int quantity = Integer.parseInt(value == null ? "" : value.trim());
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero.");
            }
            return quantity;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Quantity must be a whole number, e.g. 1 or 12.");
        }
    }

    private static LocalDate requireDate(LocalDate value) {
        if (value == null) {
            throw new IllegalArgumentException("Please choose an expiry date.");
        }
        return value;
    }

    private static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /** Values validated inside the Save button's event filter. */
    private static final class Draft {
        private String name;
        private String category;
        private int quantity;
        private LocalDate expiryDate;
        private String notes;
    }
}

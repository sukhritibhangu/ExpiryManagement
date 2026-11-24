package gui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import model.ExpiryBand;
import model.Item;
import service.ExpiryService;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import static gui.Views.*;

final class InventoryView {
    private InventoryView() { }

    static Node build(ObservableList<Item> inventory, LocalDate today, boolean tracker,
                      Runnable add, Consumer<Item> edit, Consumer<Item> delete, Runnable undo,
                      boolean canUndo, Consumer<List<Item>> export) {
        TextField search = new TextField();
        search.setPromptText("Search products or categories");
        search.setPrefColumnCount(22);
        MenuButton categories = new MenuButton("Categories");
        MenuButton statuses = new MenuButton("Expiry status");
        Set<String> selectedCategories = new HashSet<>();
        Set<ExpiryBand> selectedStatuses = EnumSet.noneOf(ExpiryBand.class);
        FilteredList<Item> filtered = new FilteredList<>(inventory);
        Runnable apply = () -> {
            String term = search.getText().strip().toLowerCase(Locale.ROOT);
            filtered.setPredicate(item -> (item.getName().toLowerCase(Locale.ROOT).contains(term)
                    || item.getCategory().toLowerCase(Locale.ROOT).contains(term))
                    && (selectedCategories.isEmpty() || selectedCategories.contains(item.getCategory()))
                    && (selectedStatuses.isEmpty() || selectedStatuses.contains(band(item, today))));
            categories.setText(selectedCategories.isEmpty() ? "Categories" : "Categories (" + selectedCategories.size() + ")");
            statuses.setText(selectedStatuses.isEmpty() ? "Expiry status" : "Expiry status (" + selectedStatuses.size() + ")");
        };
        inventory.stream().map(Item::getCategory).distinct().sorted()
                .forEach(category -> addFilter(categories, selectedCategories, category, apply));
        for (ExpiryBand status : ExpiryBand.values()) addFilter(statuses, selectedStatuses, status, apply);
        search.textProperty().addListener((ob, old, value) -> apply.run());
        Button clear = button("Clear filters", () -> {
            search.clear();
            for (MenuButton menu : List.of(categories, statuses)) {
                menu.getItems().forEach(item -> ((CheckMenuItem) item).setSelected(false));
            }
            selectedCategories.clear();
            selectedStatuses.clear();
            apply.run();
        });
        TableView<Item> table = table(filtered, today, tracker);
        FlowPane tools = new FlowPane(8, 8, search, categories, statuses, clear);
        if (tracker) {
            return card("Expiry queue", tools, table,
                    label("Remove expired stock from use. Prioritise stock expiring soon and review replenishment.", "muted"));
        }

        VBox detail = card("Product details", label("Select a product to see its details.", "muted"));
        table.getSelectionModel().selectedItemProperty().addListener((ob, old, value) -> {
            detail.getChildren().setAll(label("Product details", "section"));
            if (value == null) {
                detail.getChildren().add(label("Select a product to see its details.", "muted"));
            } else {
                detail.getChildren().addAll(label(value.getName(), "section"),
                        label(value.getCategory() + "  ·  " + value.getQuantity() + " units  ·  Expires " + value.getExpiryDate()),
                        label("Manufactured " + display(value.getManufactured()) + "  ·  Batch " + display(value.getBatch())
                                + "  ·  Unit price " + display(value.getPrice()), "muted"),
                        label(value.getNotes().isBlank() ? "No notes" : value.getNotes(), "muted"),
                        row(button("Edit", () -> edit.accept(value)), button("Delete", () -> delete.accept(value), "danger")));
            }
        });
        table.setRowFactory(view -> {
            TableRow<Item> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) edit.accept(row.getItem());
            });
            return row;
        });
        Button addButton = button("Add product", add, "primary");
        tip(addButton, "Add product (Ctrl+N)");
        Button undoButton = button("Undo delete", undo);
        undoButton.setDisable(!canUndo);
        tools.getChildren().addAll(button("Export filtered CSV", () -> export.accept(List.copyOf(table.getItems()))),
                undoButton, addButton);
        return new VBox(14, card("Products", tools, table), detail);
    }

    private static String display(Object value) {
        return value == null || value.toString().isBlank() ? "—" : value.toString();
    }

    private static <T> void addFilter(MenuButton menu, Set<T> selected, T value, Runnable apply) {
        CheckMenuItem option = new CheckMenuItem(value.toString());
        option.selectedProperty().addListener((ob, old, checked) -> {
            if (checked) selected.add(value);
            else selected.remove(value);
            apply.run();
        });
        menu.getItems().add(option);
    }

    static TableView<Item> table(List<Item> items, LocalDate today, boolean tracker) {
        return table(new FilteredList<>(FXCollections.observableArrayList(items)), today, tracker);
    }

    private static TableView<Item> table(FilteredList<Item> filtered, LocalDate today, boolean tracker) {
        TableView<Item> table = new TableView<>();
        table.setPrefHeight(275);
        table.setPlaceholder(label("No products match this view.", "muted"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        SortedList<Item> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        TableColumn<Item, LocalDate> expiry = column("Expiry", Item::getExpiryDate);
        TableColumn<Item, ExpiryBand> status = column("Status", item -> band(item, today));
        status.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(ExpiryBand band, boolean empty) {
                super.updateItem(band, empty);
                setGraphic(empty || band == null ? null : badge(band.toString(), band.color()));
            }
        });
        table.getColumns().add(column("Product", Item::getName));
        table.getColumns().add(column("Category", Item::getCategory));
        table.getColumns().add(column("Units", Item::getQuantity));
        table.getColumns().add(expiry);
        table.getColumns().add(status);
        if (tracker) {
            TableColumn<Item, Long> countdown = column("Countdown", item -> item.daysUntilExpiry(today));
            countdown.setCellFactory(col -> new TableCell<>() {
                @Override protected void updateItem(Long days, boolean empty) {
                    super.updateItem(days, empty);
                    if (empty || days == null) {
                        setGraphic(null);
                        return;
                    }
                    ProgressBar progress = new ProgressBar(Math.max(0, Math.min(30, days)) / 30.0);
                    progress.setPrefWidth(38);
                    progress.getStyleClass().add(ExpiryBand.of(days).color());
                    setGraphic(row(progress, label(new ExpiryService().formatDaysRemaining(days))));
                }
            });
            countdown.setMinWidth(195);
            table.getColumns().add(countdown);
            table.getSortOrder().add(expiry);
        }
        return table;
    }

    private static <T> TableColumn<Item, T> column(String name, Function<Item, T> value) {
        TableColumn<Item, T> column = new TableColumn<>(name);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        return column;
    }

    static ExpiryBand band(Item item, LocalDate today) {
        return ExpiryBand.of(item.daysUntilExpiry(today));
    }
}

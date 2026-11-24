package gui;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import model.Item;
import service.LabelScanner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

import static gui.Views.*;

final class ProductForm {
    private ProductForm() { }

    static Node build(Item item, LabelScanner.Result scan, Consumer<Item> save, Runnable cancel) {
        TextField name = new TextField(item == null ? scan == null ? "" : scan.name() : item.getName());
        TextField category = new TextField(item == null ? "" : item.getCategory());
        TextField quantity = new TextField(item == null ? "1" : Integer.toString(item.getQuantity()));
        TextField price = new TextField(item == null || item.getPrice() == null ? "" : item.getPrice().toPlainString());
        TextField batch = new TextField(item == null ? scan == null ? "" : scan.batch() : item.getBatch());
        DatePicker manufactured = new DatePicker(item == null ? scan == null ? null : scan.manufactured() : item.getManufactured());
        DatePicker expires = new DatePicker(item == null ? scan == null ? null : scan.expires() : item.getExpiryDate());
        TextArea notes = new TextArea(item == null ? "" : item.getNotes());
        notes.setPrefRowCount(3);
        manufactured.setPromptText("Optional");
        expires.setPromptText("Required");
        name.setPromptText("Product name");
        category.setPromptText("Product category");
        GridPane fields = new GridPane();
        fields.setHgap(14);
        fields.setVgap(9);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setHgrow(Priority.ALWAYS);
            fields.getColumnConstraints().add(column);
        }
        fields.addRow(0, label("Product name"), label("Category"));
        fields.addRow(1, name, category);
        fields.addRow(2, label("Quantity"), label("Unit price (optional)"));
        fields.addRow(3, quantity, price);
        fields.addRow(4, label("Manufacturing date"), label("Expiry date"));
        fields.addRow(5, manufactured, expires);
        fields.addRow(6, label("Batch number (optional)"));
        fields.addRow(7, batch);
        fields.add(label("Notes (optional)"), 0, 8, 2, 1);
        fields.add(notes, 0, 9, 2, 1);
        for (Control control : new Control[]{name, category, quantity, price, batch, manufactured, expires, notes}) {
            control.setMaxWidth(Double.MAX_VALUE);
        }
        Label error = label("", "error");
        Button submit = button(item == null ? "Save product" : "Save changes", () -> {
            try {
                String product = required(name.getText(), "Product name", 200);
                String group = required(category.getText(), "Category", 100);
                int units = Integer.parseInt(quantity.getText().strip());
                if (units <= 0) throw new IllegalArgumentException("Quantity must be a positive whole number.");
                LocalDate expiry = date(expires);
                if (expiry == null) throw new IllegalArgumentException("Choose an expiry date.");
                String unitPrice = price.getText().strip();
                if (!unitPrice.isEmpty() && !unitPrice.matches("[0-9]+(?:\\.[0-9]{1,2})?")) {
                    throw new IllegalArgumentException("Enter a non-negative unit price with up to two decimal places.");
                }
                BigDecimal cost = unitPrice.isEmpty() ? null : new BigDecimal(unitPrice);
                Item draft = new Item(item == null ? 0 : item.getId(), product, group, units, expiry,
                        limited(notes.getText(), "Notes", 500), date(manufactured), cost,
                        limited(batch.getText(), "Batch number", 100));
                error.setText("");
                save.accept(draft);
            } catch (NumberFormatException e) {
                error.setText("Quantity must be a positive whole number.");
            } catch (IllegalArgumentException e) {
                error.setText(e.getMessage());
            }
        }, "primary");
        var card = card(scan == null ? "Product information" : "Review extracted information",
                fields, error, row(submit, button("Cancel", cancel)));
        if (scan != null) {
            TextArea raw = new TextArea(scan.text());
            raw.setEditable(false);
            raw.setWrapText(true);
            raw.setPrefRowCount(4);
            TitledPane extracted = new TitledPane("Text from label", raw);
            extracted.setExpanded(false);
            card.getChildren().add(1, extracted);
        }
        card.setMaxWidth(760);
        return card;
    }

    private static LocalDate date(DatePicker picker) {
        try {
            return picker.getConverter().fromString(picker.getEditor().getText().strip());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Enter a valid date in the date picker's format.", e);
        }
    }

    private static String required(String value, String field, int limit) {
        String text = limited(value, field, limit);
        if (text.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return text;
    }

    private static String limited(String value, String field, int limit) {
        String text = value.strip();
        if (text.length() > limit) throw new IllegalArgumentException(field + " must be at most " + limit + " characters.");
        return text;
    }
}

package gui;

import animation.Motion;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Item;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static gui.Views.*;

final class OverviewView {
    private final List<Item> items;
    private final LocalDate today;
    private final boolean reduced;
    private final int safe;
    private final int soon;
    private final int expired;

    OverviewView(List<Item> items, LocalDate today, boolean reduced) {
        this.items = items;
        this.today = today;
        this.reduced = reduced;
        expired = (int) items.stream().filter(i -> i.daysUntilExpiry(today) < 0).count();
        safe = (int) items.stream().filter(i -> i.daysUntilExpiry(today) > 30).count();
        soon = items.size() - expired - safe;
    }

    Node dashboard(List<String> activity, Runnable add, Runnable scan, Runnable tracker) {
        GridPane metrics = tiles(
                metric("Total products", items.size(), "Inventory records", "neutral"),
                metric("Safe", safe, "More than 30 days", "sage"),
                metric("Expiring soon", soon, "Today through 30 days", "amber"),
                metric("Expired", expired, "Past expiry date", "rose"));
        if (expired > 0 && !reduced) Motion.scale(metrics.getChildren().get(3), 1.015, 1);
        VBox value = card("Recorded inventory value", label(value(items), "metric"));
        VBox actions = card("Quick actions", new FlowPane(8, 8,
                button("Add product", add, "primary"), button("Scan a label", scan), button("Review expiries", tracker)));
        List<Item> upcoming = items.stream().filter(i -> !i.getExpiryDate().isBefore(today))
                .sorted(Comparator.comparing(Item::getExpiryDate)).limit(5).toList();
        VBox due = card("Upcoming expiries");
        if (upcoming.isEmpty()) due.getChildren().add(label("No upcoming expiries.", "muted"));
        for (Item item : upcoming) {
            var band = InventoryView.band(item, today);
            due.getChildren().add(row(label(item.getName()), space(),
                    badge(item.getExpiryDate().toString(), band.color())));
        }
        VBox recent = card("Recent activity");
        if (activity.isEmpty()) recent.getChildren().add(label("Changes made in this session appear here.", "muted"));
        activity.forEach(entry -> recent.getChildren().add(label(entry, "muted")));
        return new VBox(15, metrics, tiles(value, actions),
                tiles(card("Category distribution", categories()), card("Inventory health", health())),
                tiles(due, recent));
    }

    Node analytics(Runnable export) {
        long units = items.stream().mapToLong(Item::getQuantity).sum();
        List<Item> atRisk = items.stream().filter(i -> i.daysUntilExpiry(today) <= 30).toList();
        return new VBox(15, row(button("Export inventory CSV", export, "primary")),
                tiles(card("Total quantity", label(units + " units", "metric")),
                        card("Potential wastage value", label(value(atRisk), "metric"),
                                label("Expired stock and stock due within 30 days", "muted"))),
                tiles(card("Category distribution", categories()), card("Inventory health", health())),
                tiles(card("Quantity by category", quantities()), card("Recorded expiries by month", trends(),
                        label("Dates from current inventory records", "muted"))));
    }

    private VBox metric(String title, int target, String caption, String color) {
        Label number = label("", "metric");
        Motion.count(number, target, reduced);
        return card("", badge(title, color), number, label(caption, "muted", "metric-caption"));
    }

    private GridPane tiles(Node... nodes) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        double minimum = nodes.length == 4 ? 157 : 315;
        int[] visibleColumns = {0};
        java.util.function.IntConsumer arrange = columns -> {
            if (columns == visibleColumns[0]) return;
            visibleColumns[0] = columns;
            grid.getColumnConstraints().clear();
            for (int column = 0; column < columns; column++) {
                ColumnConstraints width = new ColumnConstraints();
                width.setHgrow(Priority.ALWAYS);
                width.setPercentWidth(100.0 / columns);
                grid.getColumnConstraints().add(width);
            }
            for (int i = 0; i < nodes.length; i++) {
                GridPane.setColumnIndex(nodes[i], i % columns);
                GridPane.setRowIndex(nodes[i], i / columns);
            }
        };
        for (int i = 0; i < nodes.length; i++) {
            VBox card = (VBox) nodes[i];
            card.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            card.setMinWidth(0);
            grid.getChildren().add(card);
            Motion.enter(card, reduced, 0, i * 45);
        }
        arrange.accept(nodes.length);
        grid.widthProperty().addListener((ob, old, value) -> {
            int columns = Math.max(1, Math.min(nodes.length, (int) ((value.doubleValue() + 12) / (minimum + 12))));
            arrange.accept(nodes.length == 4 && columns == 3 ? 2 : columns);
        });
        return grid;
    }

    private String value(List<Item> products) {
        if (products.stream().noneMatch(i -> i.getPrice() != null)) return "—";
        BigDecimal total = products.stream().map(Item::inventoryValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        return String.format("%,.2f", total);
    }

    private Node categories() {
        var data = FXCollections.<PieChart.Data>observableArrayList();
        items.stream().collect(Collectors.groupingBy(Item::getCategory, TreeMap::new, Collectors.counting()))
                .forEach((category, count) -> data.add(new PieChart.Data(category, count)));
        return pie(data);
    }

    private Node health() {
        var data = FXCollections.<PieChart.Data>observableArrayList();
        data.addAll(new PieChart.Data("Safe (" + safe + ")", safe),
                new PieChart.Data("Due soon (" + soon + ")", soon),
                new PieChart.Data("Expired (" + expired + ")", expired));
        return pie(data);
    }

    private Node pie(javafx.collections.ObservableList<PieChart.Data> data) {
        if (items.isEmpty()) return label("Add inventory to display this chart.", "muted");
        PieChart chart = new PieChart();
        chart.setPrefSize(300, 225);
        chart.setLabelsVisible(false);
        chart.setAnimated(!reduced);
        chart.setData(data);
        return chart;
    }

    private Node quantities() {
        if (items.isEmpty()) return label("Add inventory to display this chart.", "muted");
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.setPrefSize(300, 240);
        chart.setLegendVisible(false);
        chart.setAnimated(!reduced);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        items.stream().collect(Collectors.groupingBy(Item::getCategory, TreeMap::new,
                        Collectors.summingLong(Item::getQuantity)))
                .forEach((category, count) -> series.getData().add(new XYChart.Data<>(category, count)));
        chart.getData().add(series);
        return chart;
    }

    private Node trends() {
        if (items.isEmpty()) return label("Add inventory to display this chart.", "muted");
        LineChart<String, Number> chart = new LineChart<>(new CategoryAxis(), new NumberAxis());
        chart.setPrefSize(300, 240);
        chart.setLegendVisible(false);
        chart.setAnimated(!reduced);
        var months = items.stream().collect(Collectors.groupingBy(i -> YearMonth.from(i.getExpiryDate()),
                TreeMap::new, Collectors.counting()));
        YearMonth first = YearMonth.from(today);
        DateTimeFormatter format = DateTimeFormatter.ofPattern("MMM yy");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int offset = -2; offset <= 5; offset++) {
            YearMonth month = first.plusMonths(offset);
            series.getData().add(new XYChart.Data<>(format.format(month), months.getOrDefault(month, 0L)));
        }
        chart.getData().add(series);
        return chart;
    }
}

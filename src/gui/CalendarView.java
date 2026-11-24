package gui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.ExpiryBand;
import model.Item;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static gui.Views.*;

final class CalendarView {
    private final LocalDate today;
    private final Map<LocalDate, List<Item>> byDate;
    private final GridPane grid = new GridPane();
    private final Label caption = label("", "section");
    private final VBox details = card("");
    private YearMonth month;
    private LocalDate selected;

    CalendarView(List<Item> items, LocalDate today) {
        this.today = today;
        month = YearMonth.from(today);
        selected = today;
        byDate = items.stream().collect(Collectors.groupingBy(Item::getExpiryDate));
        grid.setHgap(6);
        grid.setVgap(6);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 7);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
    }

    Node build() {
        Button previous = button("‹", () -> move(-1));
        Button next = button("›", () -> move(1));
        tip(previous, "Previous month");
        tip(next, "Next month");
        render();
        return new VBox(14, card("", row(previous, caption, next, space(), button("Today", () -> {
            month = YearMonth.from(today);
            selected = today;
            render();
        })), grid), details);
    }

    private void move(int offset) {
        month = month.plusMonths(offset);
        selected = month.atDay(1);
        render();
    }

    private void render() {
        caption.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.getYear());
        grid.getChildren().clear();
        for (DayOfWeek day : DayOfWeek.values()) {
            grid.add(label(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()), "muted"), day.getValue() - 1, 0);
        }
        int shift = month.atDay(1).getDayOfWeek().getValue() - 1;
        for (int i = 1; i <= month.lengthOfMonth(); i++) {
            LocalDate date = month.atDay(i);
            List<Item> expiring = byDate.getOrDefault(date, List.of());
            String indicator = expiring.isEmpty() ? "" : "\n" + expiring.size() + " due";
            String color = expiring.isEmpty() ? "neutral"
                    : ExpiryBand.of(java.time.temporal.ChronoUnit.DAYS.between(today, date)).color();
            Button button = button(i + indicator, () -> {
                selected = date;
                render();
            }, "calendar-day", color);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER);
            if (date.equals(selected)) button.getStyleClass().add("selected");
            tip(button, date + " · " + expiring.size() + " products");
            int slot = i - 1 + shift;
            grid.add(button, slot % 7, 1 + slot / 7);
        }
        List<Item> due = byDate.getOrDefault(selected, List.of());
        details.getChildren().setAll(label("Expiries on " + selected, "section"));
        if (due.isEmpty()) details.getChildren().add(label("No products expire on this date.", "muted"));
        else details.getChildren().add(InventoryView.table(due, today, false));
    }
}

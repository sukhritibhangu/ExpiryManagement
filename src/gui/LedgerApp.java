package gui;

import animation.Motion;
import config.LedgerPreferences;
import javafx.animation.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import model.Item;
import service.ExpiryService;
import service.ItemService;
import service.LabelScanner;
import storage.CsvStorage;
import storage.StorageFactory;
import util.AppLogging;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static gui.Views.*;

public class LedgerApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(LedgerApp.class.getName());
    private static final String LEAF = "M2 14 Q2 2 17 2 Q17 15 7 16 L3 18 L2 17 L10 9 L9 8 Z";
    private static final double SIDEBAR_WIDTH = 211;
    private static final double COLLAPSED_WIDTH = 62;

    private enum Page {
        DASHBOARD("Dashboard", "Live inventory and expiry overview", "M1 1H8V8H1ZM10 1H17V8H10ZM1 10H8V17H1ZM10 10H17V17H10Z"),
        INVENTORY("Inventory", "Search and manage your product records", "M1 3H17V6H1ZM2 7H16V17H2ZM7 9V11H11V9Z"),
        ADD("Add Product", "Create a record for a product or batch", "M8 1H10V8H17V10H10V17H8V10H1V8H8Z"),
        SCAN("Scan Product", "Read a label image and review the extracted fields", "M1 1H6V3H3V6H1ZM12 1H17V6H15V3H12ZM1 12H3V15H6V17H1ZM15 12H17V17H12V15H15ZM2 8H16V10H2Z"),
        TRACKER("Expiry Tracker", "Review stock in order of expiry urgency", "M9 1A8 8 0 1 0 9 17A8 8 0 0 0 9 1ZM8 4H10V8L13 10L12 12L8 9Z"),
        CALENDAR("Calendar", "Explore product expiry dates by month", "M1 3H4V1H6V3H12V1H14V3H17V17H1ZM3 7V15H15V7Z"),
        ANALYTICS("Analytics", "Explore product quantities, value and expiry trends", "M1 16H17V18H1ZM2 9H5V15H2ZM7 5H10V15H7ZM12 1H15V15H12Z"),
        NOTIFICATIONS("Notifications", "Review expiry reminders for this workspace", "M9 1Q14 1 14 7V11L17 14H1L4 11V7Q4 1 9 1ZM7 15H11Q11 18 9 18Q7 18 7 15Z"),
        SETTINGS("Settings", "Manage reminders, workspace name and motion preferences", "M6 1H12L13 4L16 4L18 9L16 14L13 14L12 17H6L5 14L2 14L0 9L2 4L5 4ZM9 6A3 3 0 1 0 9 12A3 3 0 1 0 9 6Z");

        final String title;
        final String description;
        final String icon;

        Page(String title, String description, String icon) {
            this.title = title;
            this.description = description;
            this.icon = icon;
        }
    }

    private record Workspace(ItemService service, LedgerPreferences preferences, List<Item> items) { }

    private final ExpiryService expiry = new ExpiryService();
    private final ObservableList<Item> rows = FXCollections.observableArrayList();
    private final Deque<String> activity = new ArrayDeque<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "ledger-worker");
        thread.setDaemon(true);
        return thread;
    });
    private final EnumMap<Page, Button> navigation = new EnumMap<>(Page.class);
    private final VBox sidebar = new VBox(7);
    private final VBox page = new VBox();
    private final ScrollPane scroll = new ScrollPane(page);
    private final Label profile = label("", "profile");
    private final Label brand = label("Expiry Alert\nLedger", "brand");
    private final Label notice = label("", "muted");
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Button notifications = button("Notifications", () -> navigate(Page.NOTIFICATIONS));
    private final Button collapse = button("", () -> setCollapsed(!collapsedState()));
    private Stage stage;
    private ItemService service;
    private LedgerPreferences preferences;
    private WorkspaceFiles files;
    private LocalDate today = LocalDate.now();
    private Timeline clock;
    private Page current = Page.DASHBOARD;
    private boolean collapsed;
    private boolean compact;
    private boolean busy;
    private Item editing;
    private Item deleted;
    private LabelScanner.Result scanned;

    public static void main(String[] args) { launch(args); }

    protected ItemService openInventory() { return new ItemService(StorageFactory.create(), expiry); }

    protected Path preferencesPath() { return Path.of("data/profile.properties"); }

    protected WorkspaceFiles workspaceFiles() { return WorkspaceFiles.desktop(); }

    @Override public void start(Stage stage) {
        AppLogging.configure();
        this.stage = stage;
        files = workspaceFiles();
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("workspace");
        sidebar.getStyleClass().add("sidebar");
        sidebar.setMinWidth(Region.USE_PREF_SIZE);
        sidebar.setMaxWidth(Region.USE_PREF_SIZE);
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setFillWidth(true);

        StackPane logo = new StackPane(icon(LEAF));
        logo.getStyleClass().add("brand-mark");
        logo.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        sidebar.getChildren().add(row(logo, brand));
        for (Page item : Page.values()) {
            Button button = button(item.title, () -> navigate(item));
            button.setGraphic(icon(item.icon));
            button.setMaxWidth(Double.MAX_VALUE);
            tip(button, item.title);
            navigation.put(item, button);
            sidebar.getChildren().add(button);
        }
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().addAll(spacer, profile);
        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToHeight(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.getStyleClass().add("sidebar-scroll");
        shell.setLeft(sidebarScroll);

        collapse.setGraphic(icon("M1 3H17V5H1ZM1 8H13V10H1ZM1 13H17V15H1Z"));
        tip(collapse, "Toggle navigation");
        Label date = label(dateLabel(), "muted");
        var top = row(collapse, date, space(), notifications);
        top.getStyleClass().add("topbar");
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        page.getStyleClass().add("page");
        progress.setMaxSize(19, 19);
        progress.managedProperty().bind(progress.visibleProperty());
        var footer = row(progress, notice);
        footer.getStyleClass().add("footer");
        footer.visibleProperty().bind(notice.textProperty().isNotEmpty());
        footer.managedProperty().bind(footer.visibleProperty());
        BorderPane main = new BorderPane(scroll, top, null, footer, null);
        shell.setCenter(main);
        Scene scene = new Scene(shell, 1150, 760);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/gui/ledger.css")).toExternalForm());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), () -> navigate(Page.ADD));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.I, KeyCombination.SHORTCUT_DOWN), () -> navigate(Page.INVENTORY));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.D, KeyCombination.SHORTCUT_DOWN), () -> navigate(Page.DASHBOARD));
        scene.widthProperty().addListener((ob, old, width) -> {
            boolean narrow = width.doubleValue() < 820;
            if (narrow != compact) {
                compact = narrow;
                updateSidebar(true);
            }
        });
        stage.setTitle("Expiry Alert Ledger");
        stage.setMinWidth(640);
        stage.setMinHeight(500);
        stage.setScene(scene);
        stage.show();
        page.getChildren().setAll(label("Opening workspace", "title"));
        loadWorkspace(logo);
        clock = new Timeline(new KeyFrame(Duration.seconds(20), event -> {
            LocalDate now = LocalDate.now();
            if (!busy && service != null && !now.equals(today)) {
                today = now;
                date.setText(dateLabel());
                updateNotifications();
                if (current != Page.ADD && current != Page.SCAN && current != Page.SETTINGS) render();
            }
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void loadWorkspace(Node logo) {
        run("Opening inventory…", () -> {
            LedgerPreferences settings = new LedgerPreferences(preferencesPath());
            ItemService inventory = openInventory();
            return new Workspace(inventory, settings, inventory.getAllItems());
        }, result -> {
            service = result.service();
            preferences = result.preferences();
            rows.setAll(result.items());
            updateSidebar(false);
            Motion.enter(logo, reduced(), -12, 0);
            navigate(Page.DASHBOARD);
        }, () -> page.getChildren().setAll(label("Workspace unavailable", "title"),
                button("Retry", () -> loadWorkspace(logo), "primary")));
    }

    private String dateLabel() { return today.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")); }

    private boolean collapsedState() { return collapsed || compact; }

    private void setCollapsed(boolean value) {
        if (compact) compact = false;
        collapsed = value;
        updateSidebar(true);
    }

    private void updateSidebar(boolean animate) {
        boolean narrow = collapsedState();
        brand.setVisible(!narrow);
        brand.setManaged(!narrow);
        profile.setVisible(!narrow);
        profile.setManaged(!narrow);
        if (preferences != null) {
            profile.setText(preferences.profile());
            stage.getScene().getProperties().put("ledger.reducedMotion", preferences.reduced());
        }
        navigation.forEach((destination, button) -> button.setText(narrow ? "" : destination.title));
        Motion.width(sidebar, narrow ? COLLAPSED_WIDTH : SIDEBAR_WIDTH, !animate || reduced());
    }

    private boolean reduced() { return preferences == null || preferences.reduced(); }

    private void navigate(Page destination) {
        if (service == null || busy) return;
        editing = null;
        scanned = null;
        current = destination;
        render();
    }

    private void render() {
        navigation.forEach((destination, button) -> {
            button.getStyleClass().remove("active");
            if (current == destination) button.getStyleClass().add("active");
        });
        String title = current == Page.ADD && editing != null ? "Edit Product" : current.title;
        String description = editing == null ? current.description : "Update an existing inventory record";
        VBox heading = new VBox(3, label(title, "title"), label(description, "muted"));
        Node content = switch (current) {
            case DASHBOARD -> new OverviewView(rows, today, reduced()).dashboard(List.copyOf(activity),
                    () -> navigate(Page.ADD), () -> navigate(Page.SCAN), () -> navigate(Page.TRACKER));
            case INVENTORY, TRACKER -> InventoryView.build(rows, today, current == Page.TRACKER,
                    () -> navigate(Page.ADD), this::edit, this::delete, this::undo, deleted != null, this::export);
            case ADD -> ProductForm.build(editing, scanned, this::save, () -> navigate(Page.INVENTORY));
            case SCAN -> scan();
            case CALENDAR -> new CalendarView(rows, today).build();
            case ANALYTICS -> new OverviewView(rows, today, reduced()).analytics(() -> export(List.copyOf(rows)));
            case NOTIFICATIONS -> notificationView();
            case SETTINGS -> settings();
        };
        page.getChildren().setAll(heading, content);
        scroll.setVvalue(0);
        Motion.enter(content, reduced(), current == Page.ADD ? 28 : 8, 0);
        updateNotifications();
    }

    private void edit(Item item) {
        editing = item;
        current = Page.ADD;
        render();
    }

    private void save(Item item) {
        run("Saving product…", () -> {
            service.saveItem(item);
            return service.getAllItems();
        }, items -> {
            rows.setAll(items);
            success((item.getId() == 0 ? "Added " : "Updated ") + item.getName());
            navigate(Page.INVENTORY);
        }, null);
    }

    private void delete(Item item) {
        VBox prompt = card("Delete product", label("Delete " + item.getName() + " from inventory?"));
        prompt.getChildren().add(row(button("Confirm delete", () -> {
            page.getChildren().remove(prompt);
            run("Deleting product…", () -> {
                if (!service.deleteItem(item.getId())) throw new IllegalStateException("Product no longer exists.");
                return service.getAllItems();
            }, items -> {
                deleted = item;
                Node tableRow = page.lookup(".table-row-cell:selected");
                if (reduced() || tableRow == null) finishDelete(items, item);
                else {
                    setBusy(true, "Updating inventory…");
                    var fade = new FadeTransition(Duration.millis(155), tableRow);
                    fade.setToValue(0);
                    fade.setOnFinished(event -> {
                        setBusy(false, "");
                        finishDelete(items, item);
                    });
                    fade.play();
                }
            }, null);
        }, "danger"), button("Cancel", () -> page.getChildren().remove(prompt))));
        page.getChildren().removeIf(node -> "delete-confirmation".equals(node.getId()));
        prompt.setId("delete-confirmation");
        page.getChildren().add(1, prompt);
        scroll.setVvalue(0);
        Motion.enter(prompt, reduced(), 10, 0);
    }

    private void finishDelete(List<Item> items, Item item) {
        rows.setAll(items);
        success("Deleted " + item.getName());
        render();
    }

    private void undo() {
        Item item = deleted;
        if (item == null) return;
        run("Restoring product…", () -> {
            service.restoreItem(item);
            return service.getAllItems();
        }, items -> {
            deleted = null;
            rows.setAll(items);
            success("Restored " + item.getName());
            render();
        }, null);
    }

    private void export(List<Item> items) {
        try {
            files.export(stage, target -> run("Exporting inventory…", () -> {
                Path inventory = Path.of("data/inventory.csv");
                if (target.toAbsolutePath().normalize().equals(inventory.toAbsolutePath().normalize())
                        || (java.nio.file.Files.exists(target) && java.nio.file.Files.exists(inventory)
                        && java.nio.file.Files.isSameFile(target, inventory))) {
                    throw new IllegalStateException("Choose a report path outside the live inventory file.");
                }
                if (!new CsvStorage(target.toString()).exportItems(items)) {
                    throw new IllegalStateException("Could not write the report to the selected location.");
                }
                return target;
            }, path -> {
                try {
                    if (files instanceof DownloadFiles download) download.deliver(stage, path);
                    notice.setText("✓ Exported inventory report");
                } catch (IllegalStateException e) {
                    notice.setText(e.getMessage());
                }
            }, () -> files.discardExport(target)));
        } catch (IllegalStateException e) {
            notice.setText(e.getMessage());
        }
    }

    public interface DownloadFiles extends WorkspaceFiles {
        void deliver(Stage stage, Path file);
    }

    private Node scan() {
        Button upload = new Button("Choose label image");
        upload.getStyleClass().add("primary");
        Rectangle beam = new Rectangle(250, 3);
        beam.getStyleClass().add("scan-track");
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(28, 28);
        StackPane strip = new StackPane(beam, indicator);
        strip.setPrefHeight(65);
        strip.setVisible(false);
        strip.managedProperty().bind(strip.visibleProperty());
        var sweep = new TranslateTransition(Duration.millis(720), beam);
        sweep.setFromY(-25);
        sweep.setToY(25);
        sweep.setAutoReverse(true);
        sweep.setCycleCount(Animation.INDEFINITE);
        files.bindImage(upload, stage, path -> {
            if (busy || current != Page.SCAN || upload.getScene() == null) {
                files.releaseImage(path);
                return;
            }
            strip.setVisible(true);
            if (!reduced()) sweep.playFromStart();
            Runnable reset = () -> {
                sweep.stop();
                strip.setVisible(false);
            };
            run("Reading label…", () -> {
                try {
                    return new LabelScanner().scan(path);
                } finally {
                    files.releaseImage(path);
                }
            }, result -> {
                reset.run();
                scanned = result;
                current = Page.ADD;
                notice.setText("Review the extracted fields before saving.");
                render();
            }, reset);
        }, notice::setText);
        return card("Label OCR", label("Choose a clear PNG or JPEG of the product label.", "muted"),
                upload, strip, button("Enter product manually", () -> navigate(Page.ADD)));
    }

    private List<Item> reminders() {
        return rows.stream().filter(item -> item.daysUntilExpiry(today) <= preferences.reminder())
                .sorted(Comparator.comparing(Item::getExpiryDate)).toList();
    }

    private String reminderKey(Item item) {
        return item.getId() + "/" + item.getExpiryDate() + "/" + InventoryView.band(item, today).name();
    }

    private void updateNotifications() {
        if (preferences == null) return;
        long unread = reminders().stream().filter(item -> !preferences.read(reminderKey(item))).count();
        notifications.setText(unread == 0 ? "Notifications" : "Notifications (" + unread + ")");
    }

    private Node notificationView() {
        List<Item> due = reminders();
        Set<String> unread = due.stream().map(this::reminderKey).filter(key -> !preferences.read(key))
                .collect(Collectors.toSet());
        Button markAll = button("Mark all as read", () -> markRead(unread));
        markAll.setDisable(unread.isEmpty());
        VBox list = card("Expiry reminders", row(label("Reminder window · " + preferences.reminder() + " days", "muted"),
                space(), markAll));
        if (due.isEmpty()) list.getChildren().add(label("No products need an expiry reminder.", "muted"));
        for (Item item : due) {
            String key = reminderKey(item);
            var band = InventoryView.band(item, today);
            Button mark = button("Mark read", () -> markRead(Set.of(key)));
            mark.setDisable(!unread.contains(key));
            list.getChildren().add(row(label(item.getName()),
                    badge(expiry.formatDaysRemaining(item.daysUntilExpiry(today)), band.color()), space(), mark));
        }
        return list;
    }

    private void markRead(Set<String> keys) {
        run("Updating notifications…", () -> {
            preferences.markRead(keys);
            return null;
        }, ignored -> render(), null);
    }

    private Node settings() {
        TextField name = new TextField(preferences.profile());
        Spinner<Integer> threshold = new Spinner<>(0, 365, preferences.reminder());
        threshold.setEditable(true);
        CheckBox reducedMotion = new CheckBox("Reduce motion");
        reducedMotion.setSelected(reduced());
        return card("Workspace preferences", label("Workspace name"), name, label("Expiry reminder window (days)"),
                threshold, reducedMotion, button("Save preferences", () -> {
                    String profileName = name.getText().strip();
                    if (profileName.isBlank() || profileName.length() > 50) {
                        notice.setText("Use a workspace name between 1 and 50 characters.");
                        return;
                    }
                    int days;
                    try {
                        days = Integer.parseInt(threshold.getEditor().getText().strip());
                        if (days < 0 || days > 365) throw new NumberFormatException();
                    } catch (NumberFormatException e) {
                        notice.setText("Enter a reminder window from 0 to 365 days.");
                        return;
                    }
                    boolean reduce = reducedMotion.isSelected();
                    run("Saving preferences…", () -> {
                        preferences.update(days, reduce, profileName);
                        return null;
                    }, ignored -> {
                        updateSidebar(false);
                        success("Preferences saved");
                        updateNotifications();
                    }, null);
                }, "primary"), label("Keyboard shortcuts", "section"),
                label("Ctrl+N Add product    Ctrl+I Inventory    Ctrl+D Dashboard", "muted"));
    }

    private void success(String message) {
        activity.addFirst(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) + "  ·  " + message);
        if (activity.size() > 5) activity.removeLast();
        notice.setText("✓ " + message);
        Motion.enter(notice, reduced(), -8, 0);
    }

    private <T> void run(String status, Callable<T> job, Consumer<T> completed, Runnable failed) {
        if (busy) return;
        setBusy(true, status);
        Task<T> task = new Task<>() {
            @Override protected T call() throws Exception { return job.call(); }
        };
        task.setOnSucceeded(event -> {
            setBusy(false, "");
            completed.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            LOGGER.log(Level.WARNING, status.replace("…", "") + " failed", error);
            setBusy(false, error.getMessage() == null ? "Operation failed. See the ledger log." : error.getMessage());
            if (failed != null) failed.run();
        });
        worker.execute(task);
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        progress.setVisible(value);
        notice.setText(message);
        page.setDisable(value);
    }

    protected void closeWorkspace() { }

    @Override public void stop() {
        if (clock != null) clock.stop();
        worker.execute(this::closeWorkspace);
        worker.shutdown();
    }
}

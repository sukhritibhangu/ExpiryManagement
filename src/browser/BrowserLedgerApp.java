package browser;

import com.jpro.webapi.WebAPI;
import config.WorkspaceAccess;
import gui.LedgerApp;
import gui.WorkspaceFiles;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import one.jpro.platform.file.ExtensionFilter;
import one.jpro.platform.file.picker.FileOpenPicker;
import service.ExpiryService;
import service.ItemService;
import storage.JdbcStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class BrowserLedgerApp extends LedgerApp {
    private static final Logger LOGGER = Logger.getLogger(BrowserLedgerApp.class.getName());
    private volatile WorkspaceAccess access;
    private volatile boolean closed;

    @Override public void start(Stage stage) {
        if (!WebAPI.isBrowser()) {
            super.start(stage);
            return;
        }
        Label title = new Label("Expiry Alert Ledger");
        title.getStyleClass().add("title");
        Label status = new Label();
        status.setWrapText(true);
        PasswordField key = new PasswordField();
        key.setPromptText("Workspace key");
        Button open = new Button("Open workspace");
        open.getStyleClass().add("primary");
        Button create = new Button("Create private workspace");
        VBox card = new VBox(12, title, new Label("Open your existing workspace or create a private one."),
                key, open, create, status);
        card.getStyleClass().add("card");
        card.setMaxSize(520, javafx.scene.layout.Region.USE_PREF_SIZE);
        open.setOnAction(event -> open(stage, card, status, key.getText().strip(), false));
        key.setOnAction(event -> open.fire());
        create.setOnAction(event -> {
            String secret = WorkspaceAccess.generateKey();
            TextField shown = new TextField(secret);
            shown.setEditable(false);
            Button continueButton = new Button("I saved my key");
            Button cancel = new Button("Cancel");
            var previous = java.util.List.copyOf(card.getChildren());
            cancel.setOnAction(cancelled -> card.getChildren().setAll(previous));
            continueButton.getStyleClass().add("primary");
            continueButton.setOnAction(confirmed -> open(stage, card, status, secret, true));
            status.setText("");
            card.getChildren().setAll(title, new Label("Save this key somewhere private. You need it to reopen your workspace."),
                    shown, continueButton, cancel, status);
            shown.requestFocus();
            shown.selectAll();
        });
        StackPane root = new StackPane(card);
        root.setPadding(new Insets(18));
        Scene scene = new Scene(root, 1000, 720);
        scene.getStylesheets().add(getClass().getResource("/gui/ledger.css").toExternalForm());
        stage.setTitle("Expiry Alert Ledger");
        stage.setScene(scene);
        stage.show();
    }

    private void open(Stage stage, VBox card, Label status, String key, boolean create) {
        card.setDisable(true);
        status.setText("Opening workspace…");
        Task<WorkspaceAccess> task = new Task<>() {
            @Override protected WorkspaceAccess call() throws IOException {
                Path root = Path.of(System.getenv().getOrDefault("LEDGER_BROWSER_DATA", "browser-data"));
                WorkspaceAccess opened = WorkspaceAccess.open(root, key, create);
                synchronized (BrowserLedgerApp.this) {
                    if (closed) {
                        opened.close();
                        throw new IOException("The browser session closed.");
                    }
                    access = opened;
                }
                return opened;
            }
        };
        task.setOnSucceeded(event -> {
            if (!closed) super.start(stage);
        });
        task.setOnFailed(event -> {
            card.setDisable(false);
            status.setText(task.getException().getMessage());
        });
        Thread worker = new Thread(task, "ledger-workspace-access");
        worker.setDaemon(true);
        worker.start();
    }

    @Override protected ItemService openInventory() {
        if (access == null) return super.openInventory();
        String path = access.directory().resolve("inventory").toAbsolutePath().toString().replace('\\', '/');
        return new ItemService(new JdbcStorage("jdbc:h2:file:" + path), new ExpiryService(), null, false);
    }

    @Override protected Path preferencesPath() {
        return access == null ? super.preferencesPath() : access.directory().resolve("profile.properties");
    }

    @Override protected WorkspaceFiles workspaceFiles() {
        if (!WebAPI.isBrowser()) return super.workspaceFiles();
        return new DownloadFiles() {
            @Override public void bindImage(Button button, Window owner, Consumer<Path> selected, Consumer<String> error) {
                FileOpenPicker picker = FileOpenPicker.create(button);
                picker.getExtensionFilters().add(new ExtensionFilter("Product images", ".png", ".jpg", ".jpeg"));
                button.getProperties().put(FileOpenPicker.class, picker);
                picker.setOnFilesSelected(sources -> {
                    if (sources.isEmpty()) return;
                    var source = sources.get(0);
                    if (source.getSize() > 12L * 1024 * 1024) {
                        error.accept("Choose a label image no larger than 12 MB.");
                        return;
                    }
                    button.setDisable(true);
                    error.accept("Uploading label image…");
                    source.uploadFileAsync().whenComplete((file, failure) -> Platform.runLater(() -> {
                        button.setDisable(false);
                        if (failure == null && file != null) selected.accept(file.toPath());
                        else error.accept("Upload failed. Choose the image again.");
                    }));
                });
            }

            @Override public void releaseImage(Path path) { remove(path); }

            @Override public void export(Window owner, Consumer<Path> selected) {
                try {
                    selected.accept(Files.createTempFile("ledger-report-", ".csv"));
                } catch (IOException e) {
                    throw new IllegalStateException("Could not create the download.", e);
                }
            }

            @Override public void discardExport(Path path) { remove(path); }

            @Override public void deliver(Stage stage, Path file) {
                try {
                    WebAPI.getWebAPI(stage).downloadURL(file.toUri().toURL(), () -> remove(file));
                } catch (IOException | RuntimeException e) {
                    remove(file);
                    throw new IllegalStateException("The browser download could not be started.", e);
                }
            }
        };
    }

    private static void remove(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            LOGGER.warning("Could not remove a temporary browser file.");
        }
    }

    @Override public void stop() {
        synchronized (this) {
            closed = true;
        }
        super.stop();
    }

    @Override protected void closeWorkspace() {
        if (access == null) return;
        try {
            access.close();
        } catch (IOException e) {
            LOGGER.warning("Could not release the workspace lock.");
        }
    }
}

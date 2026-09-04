package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import model.ExpiryStatus;
import model.Item;
import service.ExpiryService;
import service.ItemService;
import storage.CsvStorage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight HTTP Web Server and REST API using standard JDK com.sun.net.httpserver.
 * 
 * Zero external libraries required. Serves the web frontend assets and provides
 * JSON endpoints for inventory management.
 */
public class WebServer {

    private final ItemService itemService;
    private final ExpiryService expiryService;
    private final int port;
    private HttpServer server;

    public WebServer(int port) {
        this.port = port;
        this.expiryService = new ExpiryService();
        this.itemService = new ItemService(new CsvStorage(), expiryService);
    }

    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        WebServer app = new WebServer(port);
        app.start();
    }

    /**
     * Starts the HTTP server and registers endpoints.
     */
    public void start() {
        int currentPort = port;
        int maxAttempts = 5;

        for (int i = 0; i < maxAttempts; i++) {
            try {
                server = HttpServer.create(new InetSocketAddress(currentPort), 0);
                break;
            } catch (IOException e) {
                currentPort++;
            }
        }

        if (server == null) {
            System.err.println("[Error] Could not bind to port " + port + " or fallback ports.");
            return;
        }

        // Static files handler
        server.createContext("/", new StaticFileHandler());

        // API endpoints
        server.createContext("/api/items", new ItemsApiHandler());
        server.createContext("/api/alerts", new AlertsApiHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("===================================================================");
        System.out.println("       EXPIRY ALERT SYSTEM - WEB SERVER STARTED                    ");
        System.out.println("===================================================================");
        System.out.println(" Access Dashboard: http://localhost:" + currentPort);
        System.out.println(" API Endpoint:    http://localhost:" + currentPort + "/api/items");
        System.out.println(" Press Ctrl+C in console to stop the server.");
        System.out.println("===================================================================");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // HTTP HANDLERS
    // =========================================================================

    /**
     * Serves static frontend files (index.html, style.css, app.js) from the 'web/' directory.
     */
    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            Path filePath = Paths.get("web" + path);
            File file = filePath.toFile();

            if (!file.exists() || file.isDirectory()) {
                sendResponse(exchange, 404, "text/plain", "404 Not Found".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".json")) contentType = "application/json; charset=UTF-8";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = Files.readAllBytes(filePath);
            sendResponse(exchange, 200, contentType, bytes);
        }
    }

    /**
     * REST API for /api/items (GET, POST, DELETE).
     */
    private class ItemsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod().toUpperCase();

            switch (method) {
                case "OPTIONS" -> sendResponse(exchange, 204, "text/plain", new byte[0]);
                case "GET" -> handleGetItems(exchange);
                case "POST" -> handlePostItem(exchange);
                case "DELETE" -> handleDeleteItem(exchange);
                default -> sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}".getBytes(StandardCharsets.UTF_8));
            }
        }

        private void handleGetItems(HttpExchange exchange) throws IOException {
            List<Item> items = itemService.getAllItems();
            String json = itemsToJson(items);
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
        }

        private void handlePostItem(HttpExchange exchange) throws IOException {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            try {
                String name = parseJsonField(body, "name");
                String category = parseJsonField(body, "category");
                String quantityStr = parseJsonField(body, "quantity");
                String expiryDateStr = parseJsonField(body, "expiryDate");
                String notes = parseJsonField(body, "notes");

                if (name == null || name.trim().isEmpty() ||
                    category == null || category.trim().isEmpty() ||
                    quantityStr == null || expiryDateStr == null) {
                    sendJsonError(exchange, 400, "Name, category, quantity, and expiryDate are required.");
                    return;
                }

                int quantity = Integer.parseInt(quantityStr.trim());
                if (quantity <= 0) {
                    sendJsonError(exchange, 400, "Quantity must be greater than zero.");
                    return;
                }

                LocalDate expiryDate = LocalDate.parse(expiryDateStr.trim());
                Item newItem = itemService.addItem(name, category, quantity, expiryDate, notes == null ? "" : notes);

                String responseJson = itemToJson(newItem);
                sendResponse(exchange, 201, "application/json; charset=UTF-8", responseJson.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                sendJsonError(exchange, 400, "Invalid item data: " + e.getMessage());
            }
        }

        private void handleDeleteItem(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String idStr = params.get("id");

            if (idStr == null) {
                sendJsonError(exchange, 400, "Missing 'id' parameter.");
                return;
            }

            try {
                int id = Integer.parseInt(idStr);
                boolean deleted = itemService.deleteItem(id);
                if (deleted) {
                    sendResponse(exchange, 200, "application/json", "{\"success\":true}".getBytes(StandardCharsets.UTF_8));
                } else {
                    sendJsonError(exchange, 404, "Item with ID " + id + " not found.");
                }
            } catch (NumberFormatException e) {
                sendJsonError(exchange, 400, "Invalid ID format.");
            }
        }
    }

    /**
     * REST API for /api/alerts (GET).
     */
    private class AlertsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }

            List<Item> alerts = itemService.getAlertItems();
            String json = itemsToJson(alerts);
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private String itemsToJson(List<Item> items) {
        StringBuilder sb = new StringBuilder("[");
        LocalDate today = LocalDate.now();

        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            sb.append(itemToJson(item, today));
            if (i < items.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String itemToJson(Item item) {
        return itemToJson(item, LocalDate.now());
    }

    private String itemToJson(Item item, LocalDate today) {
        ExpiryStatus status = expiryService.calculateStatus(item.getExpiryDate(), today);
        long daysRemaining = expiryService.getDaysRemaining(item.getExpiryDate(), today);
        String formattedDays = expiryService.formatDaysRemaining(daysRemaining);

        return String.format(
            "{\"id\":%d,\"name\":\"%s\",\"category\":\"%s\",\"quantity\":%d,\"expiryDate\":\"%s\",\"notes\":\"%s\",\"status\":\"%s\",\"statusLabel\":\"%s\",\"statusBadge\":\"%s\",\"daysRemaining\":%d,\"formattedDays\":\"%s\"}",
            item.getId(),
            escapeJson(item.getName()),
            escapeJson(item.getCategory()),
            item.getQuantity(),
            item.getExpiryDate(),
            escapeJson(item.getNotes()),
            status.name(),
            status.getLabel(),
            status.getBadge(),
            daysRemaining,
            escapeJson(formattedDays)
        );
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private String parseJsonField(String json, String fieldName) {
        if (json == null || fieldName == null) return null;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\"?" + java.util.regex.Pattern.quote(fieldName) + "\"?\\s*:\\s*(?:\"((?:\\\\\"|[^\"])*)\"|([^,}\\]\\r\\n]+))"
        );
        java.util.regex.Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            if (matcher.group(1) != null) {
                return matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\").trim();
            } else if (matcher.group(2) != null) {
                String val = matcher.group(2).trim();
                if (val.startsWith("\"") && val.endsWith("\"") && val.length() >= 2) {
                    val = val.substring(1, val.length() - 1);
                }
                return val;
            }
        }
        return null;
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0], kv[1]);
            }
        }
        return params;
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] responseBytes) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }

    private void sendJsonError(HttpExchange exchange, int statusCode, String message) throws IOException {
        String json = "{\"error\":\"" + escapeJson(message) + "\"}";
        sendResponse(exchange, statusCode, "application/json; charset=UTF-8", json.getBytes(StandardCharsets.UTF_8));
    }
}

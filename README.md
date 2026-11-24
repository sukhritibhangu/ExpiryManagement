# Expiry Alert System 🥫⏰

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/sukhritibhangu/ExpiryManagement)

A beginner-friendly, pure **Core Java** console application designed for households, shared apartments, pantries, and small retail/pharmacy inventories to track item expiration dates, minimize food waste, and alert users about expiring goods.

Built adhering to clean **Object-Oriented Programming (OOP)** principles using modern Java (`java.time.LocalDate`) and persistent CSV local storage, with zero external libraries or heavy frameworks.

---

## 📌 Project Purpose

Every year, households and small businesses throw away significant amounts of food, medicine, and perishables simply because expiry dates go unnoticed. The **Expiry Alert System** provides:
- A straightforward command-line interface to log and organize perishable items.
- Automatic notifications every time the application launches, flagging anything already expired or expiring within 7 days.
- Full local data persistence in CSV format so records survive restarts without needing any database setup.

---

## ✨ Key Features

1. **Automatic Startup Expiry Alerts**
   - Instantly checks and alerts you on startup if any items are already expired or expiring soon.
2. **Standard Expiry Rules**
   - 🔴 **Expired:** Expiry date is before today (`< today`).
   - 🟡 **Expiring Soon:** Expiry date is within the next 7 days (`today <= expiry <= today + 7 days`).
   - 🟢 **Safe:** Expiry date is more than 7 days away (`> today + 7 days`).
3. **Interactive Console Menu**
   - `1. Add an item` - Captures name, category, quantity, expiry date, and optional notes.
   - `2. View all items` - Formats all stored items in a clear ASCII table with real-time countdowns.
   - `3. View expiry alerts` - Filters out safe items to focus only on urgent items.
   - `4. Search items` - Flexible case-insensitive search matching item names or categories.
   - `5. Delete an item` - Removes an item by ID with confirmation.
   - `6. Exit` - Safely closes the session with confirmed CSV persistence.
4. **Input Validation**
   - Strictly validates dates (`YYYY-MM-DD`) against real calendar days (e.g., rejects `2026-02-30`).
   - Ensures quantities are positive integers (> 0).
   - Validates menu selections and handles text vs numeric input gracefully without crashing.
5. **CSV Persistence & Smart Onboarding**
   - Automatically saves all changes to `data/inventory.csv`.
   - On first launch, if no CSV data is detected, realistic starter items (e.g., milk, bread, medicine, eggs) are automatically created so you can explore all features immediately.
6. **Serializable Backup**
   - Every change also writes `data/inventory.snapshot` through `ObjectOutputStream`.
   - If the CSV is empty or missing at startup, the snapshot is deserialized with `ObjectInputStream` and the CSV is rebuilt from it.
   - The CSV always wins when it has rows, so the snapshot never overwrites newer data.
7. **Structured Logging**
   - `java.util.logging` records loads, saves, adds, deletes and failures with timestamps and severity levels.
   - Console output stays readable; warnings and errors go to stderr.
8. **Optional JDBC Backend (H2)**
   - Flip one flag to store the inventory in SQL instead of CSV: `-Dledger.storage=jdbc` or `LEDGER_STORAGE=jdbc`.
   - Uses `java.sql` with prepared statements, a transaction per save and an embedded H2 file database (`data/inventory.mv.db`).
   - CSV stays the default, so no extra downloads are required for normal use.
9. **JavaFX Desktop App**
   - A native window with the same ledger: add/delete dialogs, live search, alerts, double-click details, keyboard shortcuts.
   - Built from one `pom.xml`; run it with `mvn javafx:run`.

---

## 📂 Folder Structure

```
JAVA_Project/
│
├── data/
│   ├── inventory.csv          # Shared CSV persistence (primary store)
│   └── inventory.snapshot     # Serializable backup, recreated automatically
│
├── web/                       # Modern Frontend Single Page App
│   ├── index.html             # Dashboard with alert banner, stat cards, search & modal
│   ├── style.css              # Responsive modern design & color-coded badges
│   └── app.js                 # Dynamic UI logic, instant search/filtering & REST API calls
│
├── pom.xml                    # Maven build: H2 (JDBC) + JavaFX dependencies
│
├── src/
│   ├── gui/
│   │   └── LedgerApp.java     # JavaFX Application (table, dialogs, event handlers)
│   ├── model/
│   │   ├── InventoryEntry.java # Abstract Serializable base: id, name, category, notes
│   │   ├── Expirable.java     # Interface with default daysUntilExpiry() methods
│   │   ├── ExpiryStatus.java  # Enum: EXPIRED, EXPIRING_SOON, SAFE
│   │   └── Item.java          # extends InventoryEntry, implements Expirable
│   │
│   ├── service/
│   │   ├── ExpiryService.java # Expiry rules, day calculations, and alert filtering
│   │   └── ItemService.java   # Business logic, ID auto-increment, and CRUD coordination
│   │
│   ├── storage/
│   │   ├── ItemRepository.java  # Storage interface (CSV or JDBC)
│   │   ├── CsvStorage.java    # CSV file reader/writer, escaping, and sample data seeding
│   │   ├── JdbcStorage.java   # java.sql / H2 implementation with transactions
│   │   ├── StorageFactory.java # Picks csv or jdbc from -Dledger.storage / LEDGER_STORAGE
│   │   ├── SampleData.java    # Starter items for a fresh install
│   │   └── SnapshotStorage.java # ObjectOutputStream/ObjectInputStream backup
│   │
│   ├── util/
│   │   └── AppLogging.java    # One-time java.util.logging console configuration
│   │
│   ├── app/
│   │   └── Main.java          # Terminal UI, menu loop, validation, and table printing
│   │
│   └── web/
│       └── WebServer.java     # Pure Core Java HTTP Server & REST API
│
├── compile.bat                # Compiles all packages (app, model, service, storage, web)
├── run.bat                    # Launches Console application
├── run-web.bat                # Launches Web dashboard and opens browser automatically
└── README.md                  # Project documentation
```

---

## 🛠️ Requirements

- **Java Development Kit (JDK) 17** or newer
- Terminal or Command Prompt (PowerShell, CMD, Bash, or Zsh)
- Optional: **Apache Maven 3.9+** — needed for the JDBC backend and the JavaFX app
- Optional: **H2 driver** (`h2-*.jar`) and the **JavaFX SDK** — Maven downloads both automatically; nothing else to install

---

## 🧭 Java Topics Demonstrated

| Topic | Where in the code |
| --- | --- |
| Packages and project structure | `model`, `service`, `storage`, `util`, `app`, `web` |
| Encapsulation and validation | Private fields in `InventoryEntry`/`Item`, `Objects.requireNonNull`, quantity check |
| Inheritance and abstract classes | `Item extends InventoryEntry`; abstract `getEntryType()` and `getSummary()` |
| Interfaces and default methods | `Expirable` implemented by `Item`; `HttpHandler` implemented by the web handlers |
| Method and constructor overloading | `ExpiryService.calculateStatus`/`getDaysRemaining`, `CsvStorage()`, `SnapshotStorage()`, `ItemService(...)` |
| Nested classes | `StaticFileHandler`, `ItemsApiHandler`, `AlertsApiHandler`, `ProfileApiHandler`, `LoginApiHandler` inside `WebServer` |
| Collection framework | `ArrayList`, `List`, `Map`/`HashMap`, `Optional`, `Comparator`, `Collections.unmodifiableList` |
| Generics and streams | `List<Item>`, `Optional<Item>`, `items.stream()` in startup alerts |
| Exception handling | try-with-resources, `IOException`, `NumberFormatException`, `DateTimeParseException`, corrupt-row recovery |
| File I/O | `BufferedReader`/`BufferedWriter`, `FileReader`/`FileWriter`, `Files`, `Path` |
| Serialization and deserialization | `InventoryEntry implements Serializable`, `SnapshotStorage` uses `ObjectOutputStream`/`ObjectInputStream` |
| Logging | `java.util.logging` in `ItemService`, `ExpiryService`, `CsvStorage`, `SnapshotStorage`, `WebServer`; formatted by `util.AppLogging` |
| Enums and modern Java | `ExpiryStatus`, enhanced `switch`, pattern matching for `instanceof`, lambdas |
| Build tooling | Maven (`pom.xml`) plus `compile.bat`, `run.bat`, `run-web.bat`, `Dockerfile` |
| JDBC | `JdbcStorage` (`java.sql`, prepared statements, transactions), H2 dependency in `pom.xml`; enable with `-Dledger.storage=jdbc` |
| JavaFX GUI | `gui/LedgerApp` extends `Application`; `TableView`, `Dialog`, `DatePicker`, `Alert` |
| JavaFX events | `setOnAction` buttons, search `textProperty` listener, `TableRow` double-click, `Delete` key handler, `Ctrl+N` accelerator, Save-button event filter |

All 13 syllabus topics are now represented in the codebase.

---

## 🧱 Build with Maven (JDBC + JavaFX)

Maven is optional for the console app and the web dashboard, and required for the JDBC backend and the JavaFX window.

```bash
mvn clean package      # compiles all 16 sources, builds target/expiry-alert-ledger-1.0.0.jar
mvn exec:java          # console app (CSV mode)
mvn javafx:run         # JavaFX desktop window
```

On the first run Maven downloads two dependencies into `~/.m2` (about 40 MB, one time only):

| Dependency | Version | Needed for |
| --- | --- | --- |
| `com.h2database:h2` | 2.3.232 | JDBC backend |
| `org.openjfx:javafx-controls` | 21.0.5 | JavaFX window (platform jars for your OS are fetched too) |

To run the jar Maven produced (console app, CSV mode):

```bash
java -jar target/expiry-alert-ledger-1.0.0.jar
```

## 🗄️ JDBC Storage (H2)

CSV is the default. To keep the inventory in SQL instead:

```bash
# Console
mvn exec:java -Dledger.storage=jdbc

# Web dashboard (custom main class)
mvn exec:java -Dexec.mainClass=web.WebServer -Dledger.storage=jdbc

# Plain java, after copying the driver: mvn dependency:copy-dependencies -DincludeArtifactIds=h2
java -Dledger.storage=jdbc -cp "bin:lib/*" app.Main
java -Dledger.storage=jdbc -cp "bin:lib/*" web.WebServer 8080
```

On Windows the classpath separator is `;` — `-cp "bin;lib\*"`. The same switch works as an environment variable:

```bash
LEDGER_STORAGE=jdbc LEDGER_DB_URL=jdbc:h2:./data/inventory java -cp "bin:lib/*" web.WebServer 8080
```

| Detail | Value |
| --- | --- |
| Driver | `org.h2.Driver` (loaded reflectively, so the project compiles without it) |
| Table | `items (id, name, category, quantity, expiry_date, notes)` created on first connect |
| Database file | `data/inventory.mv.db` |
| Saving | `DELETE` + batched `INSERT` inside one transaction, rolled back on failure |
| CSV while JDBC is active | Left untouched; the serializable snapshot is still written after every change |
| Missing driver | Clear error telling you to add `h2-*.jar` to the classpath, or use `-Dledger.storage=csv` |

## 🖥️ JavaFX Desktop App

```bash
mvn javafx:run
```

Needs a graphical desktop session (it cannot start in a headless terminal — you will see `Unable to open DISPLAY`). What the window offers:

- Table of every item with colour-coded rows (red = expired, amber = expiring within 7 days).
- **Add item** dialog with validation, **Delete** with confirmation, **Refresh**, **Alerts** summary.
- Live filtering as you type in the search field; double-click a row for full details.
- `Ctrl+N` opens the add dialog, `Delete` removes the selected row.
- Uses the same `ItemService`, so CSV/JDBC selection and the snapshot backup behave identically.

## 🚀 How to Compile and Run

Open your terminal in the root directory of the project (`JAVA_Project`):

### 1. Compile the Source Code

Compile all `.java` files into a `bin` directory:

#### On Windows (PowerShell / CMD):
```cmd
javac -d bin src\model\*.java src\service\*.java src\storage\*.java src\util\*.java src\app\*.java src\web\*.java
```
*Or simply run the helper script:*
```cmd
.\compile.bat
```

#### On Linux / macOS (Bash / Zsh):
```bash
javac -d bin src/model/*.java src/service/*.java src/storage/*.java src/util/*.java src/app/*.java src/web/*.java
```

### 2. Run the Application

Execute the `Main` class with the compiled `bin` classpath:

#### Windows / Linux / macOS:
```bash
java -cp bin app.Main
```
*Or on Windows, run the one-click launcher:*
```cmd
.\run.bat
```

---

## 🖥️ Example Console Output

### 1. Startup & Expiry Warning
```
===================================================================
                    EXPIRY ALERT SYSTEM                            
        Smart Household & Small Inventory Expiry Tracker           
===================================================================
*******************************************************************
  * AUTOMATIC EXPIRY ALERT: 4 item(s) need immediate attention! *
  * [!] EXPIRED: 1 item(s) have passed their expiration date!    *
  * [!] EXPIRING SOON: 3 item(s) expire within the next 7 days!  *
*******************************************************************

-------------------------------------------------------------------
 1. Add an item
 2. View all items
 3. View expiry alerts
 4. Search items
 5. Delete an item
 6. Exit
-------------------------------------------------------------------
Enter your choice (1-6): 2
```

### 2. Viewing All Items Table
```
--- All Inventory Items ---
+-----+----------------------+------------+-----+------------+----------------------+------------------+---------------------------+
| ID  | Name                 | Category   | Qty | Expiry     | Days Left            | Status           | Notes                     |
+-----+----------------------+------------+-----+------------+----------------------+------------------+---------------------------+
| 1   | Whole Wheat Bread    | Bakery     | 1   | 2026-09-02 | Expired 2 days ago   | [EXPIRED]        | Check for mold before ... |
| 2   | Fresh Milk (1L)      | Dairy      | 2   | 2026-09-06 | 2 days left          | [EXPIRING SOON]  | Store on middle refrig... |
| 3   | Greek Yogurt (4-pack)| Dairy      | 1   | 2026-09-09 | 5 days left          | [EXPIRING SOON]  | Berry flavor              |
| 4   | Farm Fresh Eggs      | Poultry    | 12  | 2026-09-10 | 6 days left          | [EXPIRING SOON]  | Grade A free-range        |
| 5   | Paracetamol 500mg    | Medicine   | 20  | 2027-03-04 | 181 days left        | [SAFE]           | First-aid kit medicine... |
| 6   | Extra Virgin Olive...| Pantry     | 1   | 2027-09-04 | 365 days left        | [SAFE]           | Dark bottle in dry pan... |
+-----+----------------------+------------+-----+------------+----------------------+------------------+---------------------------+
Summary: Total: 6 | Expired: 1 | Expiring Soon (<= 7 days): 3 | Safe: 2
```

### 3. Adding a New Item
```
Enter your choice (1-6): 1

--- Add a New Item ---
Enter item name: Cheddar Cheese
Enter category (e.g. Dairy, Medicine, Pantry, Bakery): Dairy
Enter quantity: 2
Enter expiry date (YYYY-MM-DD): 2026-09-25
Enter notes (optional, press Enter to skip): Sharp cheddar block

[Success] Item added successfully!
  ID: 7 | Name: Cheddar Cheese | Status: [SAFE] (21 days left)
```

### 4. Searching Items
```
Enter your choice (1-6): 4

--- Search Inventory ---
Enter item name or category to search: dairy
Found 3 matching item(s):

+-----+----------------------+------------+-----+------------+----------------------+------------------+---------------------------+
| ID  | Name                 | Category   | Qty | Expiry     | Days Left            | Status           | Notes                     |
+-----+----------------------+------------+-----+------------+----------------------+------------------+---------------------------+
| 2   | Fresh Milk (1L)      | Dairy      | 2   | 2026-09-06 | 2 days left          | [EXPIRING SOON]  | Store on middle refrig... |
| 3   | Greek Yogurt (4-pack)| Dairy      | 1   | 2026-09-09 | 5 days left          | [EXPIRING SOON]  | Berry flavor              |
| 7   | Cheddar Cheese       | Dairy      | 2   | 2026-09-25 | 21 days left         | [SAFE]           | Sharp cheddar block       |
+-----+----------------------+------------+-----+------------+----------------------+------------------+---------------------------+
```

---

## 🌐 Web Dashboard & Label Scanner

In addition to the console interface, the project includes a **responsive web dashboard** with label scanning, user authentication and dark/light themes, powered by a 100% pure Java HTTP server (`com.sun.net.httpserver.HttpServer`).

### Interface:
- **Desktop and phone friendly:** a floating dock navigation on every screen size, a full ledger table on wide screens, and the same ledger re-flowed into status-striped cards on phones.
- **Two reading engines:** *Vision AI* (Google Gemini) for stamped, embossed and dot-matrix dates, or *Offline OCR* (Tesseract.js WebAssembly) which needs no key and runs entirely in the browser.
- **Live scanner feedback:** animated scan line, progress bar, per-model diagnostics, and a raw-output panel so you can see exactly what the engine read.
- **Date safety:** manufacturing dates (`Mfg`, `Mfd`, `Pkd`) are ignored in favour of expiry cues (`Exp`, `BB`, `Best Before`, `Use by`).
- **Simple custom authentication:** create your own username and password on first launch to unlock the ledger, with persistent credentials and profile editing.
- **Alerts and themes:** immediate expiry ribbon plus optional browser notifications, and a Light/Dark switcher that is remembered locally.
- **Shared data:** syncs with `data/inventory.csv` and the console application.

### 🔑 Entering your Gemini API key (Vision AI)

Vision AI is optional. Without a key, switch the engine to **Offline OCR** and everything still works.

1. Open <https://aistudio.google.com/apikey> and sign in with any Google account.
2. Click **Create API key**, choose (or let it create) a project, then copy the key — it starts with `AIza` and is about 39 characters long.
3. In the app open the **Scan** tab. Under **Gemini API key**, paste the key into the field and press **Save** (or Enter). A green `saved` chip appears next to the label.
4. Press **Test**. This calls Google directly, verifies the key, and lists the vision models it can reach. A green `key works` chip means you are ready — press **Scan** on any label photo.

Where the key lives, and what to expect:

| Detail | Value |
| --- | --- |
| Storage | Your browser only: `localStorage` key `geminiApiKey` |
| Sent to | `generativelanguage.googleapis.com` (Google), never to this app's Java server |
| Default model | `gemini-2.5-flash`, falling back through `2.5-flash-lite`, `2.0-flash`, `2.0-flash-lite`, `flash-latest` |
| Cost | The free tier is enough for label scanning |
| Clearing it | Clear the field and press Save, or clear site data for the page |

Troubleshooting (the app shows the same guidance in the **Diagnostics log** under the key field):

| Message | Fix |
| --- | --- |
| `API_KEY_INVALID` / “API key not valid” | Re-copy the key; remove quotes, spaces or line breaks |
| “API has not been used in project … or it is disabled” | Enable the **Generative Language API** for that key's project in Google Cloud Console |
| `PERMISSION_DENIED` | Remove API restrictions from the key, or create an unrestricted key |
| `RESOURCE_EXHAUSTED` / quota | Wait a minute, or use Offline OCR |
| Nothing happens on Scan | Open the Diagnostics log — every model attempt and Google's own error is recorded there |

### How to Launch the Web Frontend:

#### Windows (One-Click):
```cmd
.\run-web.bat
```
*This will automatically compile the code, start the server, and open `http://localhost:8080` in your default browser.*

#### Manual Launch (Windows / Linux / macOS):
```bash
java -cp bin web.WebServer 8080
```
Then navigate to **`http://localhost:8080`** in Chrome, Edge, or Firefox.

### Built-in REST API Endpoints:
- `GET /api/items` — Returns all items as JSON with calculated days remaining and status badges.
- `GET /api/alerts` — Returns only urgent items needing attention (Expired and Expiring Soon).
- `POST /api/items` — Adds a new item to inventory (persisted to CSV).
- `DELETE /api/items?id={id}` — Deletes an item by ID (persisted to CSV).
- `GET /api/profile` — Checks if an account is set up and returns public profile metadata.
- `POST /api/profile` — Saves or updates user credentials.
- `POST /api/login` — Verifies user credentials.

---

## 🚀 Free Cloud Deployment (Render.com)

You can deploy this application for free on **[Render](https://render.com/)** using Docker:

1. Sign up / Log in to [Render](https://render.com/) (using your GitHub account).
2. Click **New +** &rarr; **Web Service**.
3. Select your repository: **`sukhritibhangu/ExpiryManagement`**.
4. Render will auto-detect the `Dockerfile`:
   - **Name:** `expiry-management` (or any name you choose)
   - **Runtime:** `Docker`
   - **Instance Type:** `Free`
5. Click **Deploy Web Service**!
6. In ~2 minutes, your live URL will be active (e.g. `https://expiry-management.onrender.com`).

---

## 🧩 Software Design Principles

- **Separation of Concerns:** Distinct model, service, storage, and presentation layers.
- **Defensive Programming:** Null checks, empty string validation, and numeric/date format validation with friendly re-prompts.
- **Encapsulation:** Class properties are private and manipulated only via validated getters and setters.
- **Portability:** Uses platform-independent path resolution and ISO-8601 local dates.

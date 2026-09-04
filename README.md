# Expiry Alert System 🥫⏰

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

---

## 📂 Folder Structure

```
JAVA_Project/
│
├── data/
│   └── inventory.csv          # Shared CSV persistence
│
├── web/                       # Modern Frontend Single Page App
│   ├── index.html             # Dashboard with alert banner, stat cards, search & modal
│   ├── style.css              # Responsive modern design & color-coded badges
│   └── app.js                 # Dynamic UI logic, instant search/filtering & REST API calls
│
├── src/
│   ├── model/
│   │   ├── ExpiryStatus.java  # Enum: EXPIRED, EXPIRING_SOON, SAFE
│   │   └── Item.java          # Item entity with getters, setters, and validation
│   │
│   ├── service/
│   │   ├── ExpiryService.java # Expiry rules, day calculations, and alert filtering
│   │   └── ItemService.java   # Business logic, ID auto-increment, and CRUD coordination
│   │
│   ├── storage/
│   │   └── CsvStorage.java    # CSV file reader/writer, escaping, and sample data seeding
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

---

## 🚀 How to Compile and Run

Open your terminal in the root directory of the project (`JAVA_Project`):

### 1. Compile the Source Code

Compile all `.java` files into a `bin` directory:

#### On Windows (PowerShell / CMD):
```cmd
javac -d bin src\model\*.java src\service\*.java src\storage\*.java src\app\*.java
```
*Or simply run the helper script:*
```cmd
.\compile.bat
```

#### On Linux / macOS (Bash / Zsh):
```bash
javac -d bin src/model/*.java src/service/*.java src/storage/*.java src/app/*.java
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

## 🌐 Web Dashboard (Simple Frontend)

In addition to the console interface, the project includes a lightweight, modern web dashboard built with vanilla HTML5, CSS3, and JavaScript, powered by a 100% pure Java HTTP server (`com.sun.net.httpserver.HttpServer`).

### Features of the Web Dashboard:
- **Live Automatic Alerts:** Prominently alerts you about expired items and items expiring within 7 days.
- **Metric Cards:** Real-time counts for Total, Expired, Expiring Soon, and Safe items.
- **Instant Search & Filters:** Type to search instantly across names, categories, or notes, or filter by category and status tabs.
- **Add Item Dialog:** Clean modal popup with datepicker and built-in validation.
- **Delete with Confirmation:** Easily remove items directly from the table.
- **Shared Data:** Seamlessly syncs with `data/inventory.csv` and the console application.

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

---

## 🧩 Software Design Principles

- **Separation of Concerns:** Distinct model, service, storage, and presentation layers.
- **Defensive Programming:** Null checks, empty string validation, and numeric/date format validation with friendly re-prompts.
- **Encapsulation:** Class properties are private and manipulated only via validated getters and setters.
- **Portability:** Uses platform-independent path resolution and ISO-8601 local dates.

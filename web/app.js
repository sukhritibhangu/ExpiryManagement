/**
 * Expiry Alert Ledger - Master Application Controller
 * 
 * Features:
 * - Simple Custom Username & Password Authentication & Profile Management
 * - Label reading via Google Gemini (Vision AI) with model discovery,
 *   retries and a human-readable diagnostics log
 * - Offline OCR via Tesseract.js as a no-key fallback
 * - Expiry Date & Product Name Extraction Engine
 * - Responsive shell: sticky top bar, floating dock, ledger cards on phones
 * - Light & Dark themes with local persistence
 * - Expiry Notification Alerts on Login
 * - Zero Keyboard Emojis (Vector SVG Only)
 */

document.addEventListener("DOMContentLoaded", () => {

  // Global State
  let allItems = [];
  let currentStatusFilter = "ALL";
  let currentCategory = "";
  let searchQuery = "";
  let currentUser = null;
  let ocrImageFile = null;
  let isSetupMode = false;

  // DOM Elements - Auth
  const authOverlay = document.getElementById("authOverlay");
  const loginForm = document.getElementById("loginForm");
  const authBanner = document.getElementById("authBanner");
  const authUsername = document.getElementById("authUsername");
  const authPassword = document.getElementById("authPassword");
  const authDisplayName = document.getElementById("authDisplayName");
  const displayNameField = document.getElementById("displayNameField");
  const authSubmitBtn = document.getElementById("authSubmitBtn");
  const toggleAuthModeBtn = document.getElementById("toggleAuthModeBtn");
  const bookApp = document.getElementById("bookApp");
  const userDisplayName = document.getElementById("userDisplayName");
  const logoutBtn = document.getElementById("logoutBtn");

  // DOM Elements - Theme
  const themeToggleBtn = document.getElementById("themeToggleBtn");
  const themeLabel = document.getElementById("themeLabel");

  // DOM Elements - Tabs
  const ribbonTabs = document.querySelectorAll(".ribbon-tab");
  const tabContents = document.querySelectorAll(".tab-content");

  // DOM Elements - Login Expiry Alert
  const loginAlertBanner = document.getElementById("loginAlertBanner");
  const loginAlertTitle = document.getElementById("loginAlertTitle");
  const loginAlertBody = document.getElementById("loginAlertBody");
  const viewUrgentLedgerBtn = document.getElementById("viewUrgentLedgerBtn");
  const dismissAlertBtn = document.getElementById("dismissAlertBtn");

  // DOM Elements - OCR Scanner
  const dropZone = document.getElementById("dropZone");
  const ocrFileInput = document.getElementById("ocrFileInput");
  const dropzoneEmpty = document.getElementById("dropzoneEmpty");
  const previewContainer = document.getElementById("previewContainer");
  const scannedImagePreview = document.getElementById("scannedImagePreview");
  const laserScanner = document.getElementById("laserScanner");
  const ocrProgressCard = document.getElementById("ocrProgressCard");
  const ocrProgressBar = document.getElementById("ocrProgressBar");
  const ocrStatusText = document.getElementById("ocrStatusText");
  const ocrPercent = document.getElementById("ocrPercent");
  const scanActionsFooter = document.getElementById("scanActionsFooter");
  const browsePhotoBtn = document.getElementById("browsePhotoBtn");
  const cameraPhotoBtn = document.getElementById("cameraPhotoBtn");
  const runScanBtn = document.getElementById("runScanBtn");
  const runScanBtnText = document.getElementById("runScanBtnText");
  const resetScanBtn = document.getElementById("resetScanBtn");
  const ocrConfidenceBadge = document.getElementById("ocrConfidenceBadge");
  const ocrAcceptForm = document.getElementById("ocrAcceptForm");
  const ocrExtractedName = document.getElementById("ocrExtractedName");
  const ocrExtractedCategory = document.getElementById("ocrExtractedCategory");
  const ocrExtractedQuantity = document.getElementById("ocrExtractedQuantity");
  const ocrExtractedDate = document.getElementById("ocrExtractedDate");
  const ocrExtractedNotes = document.getElementById("ocrExtractedNotes");
  const addScannedItemBtn = document.getElementById("addScannedItemBtn");
  const rawOcrOutput = document.getElementById("rawOcrOutput");

  // DOM Elements - Vision AI Engine Switcher & Key Bar
  const engineAiBtn = document.getElementById("engineAiBtn");
  const engineOcrBtn = document.getElementById("engineOcrBtn");
  const aiKeyStrip = document.getElementById("aiKeyStrip");
  const geminiApiKeyInput = document.getElementById("geminiApiKeyInput");
  const saveApiKeyBtn = document.getElementById("saveApiKeyBtn");
  const testApiKeyBtn = document.getElementById("testApiKeyBtn");
  const apiKeyStatus = document.getElementById("apiKeyStatus");
  const aiDiagnostics = document.getElementById("aiDiagnostics");
  const KEY_STORAGE = "geminiApiKey";
  let activeEngine = localStorage.getItem("scanEngineMode") || "ai";

  // DOM Elements - Manual Add Form
  const manualAddForm = document.getElementById("manualAddForm");
  const manualName = document.getElementById("manualName");
  const manualCategory = document.getElementById("manualCategory");
  const manualQuantity = document.getElementById("manualQuantity");
  const manualDate = document.getElementById("manualDate");
  const manualNotes = document.getElementById("manualNotes");

  // DOM Elements - Ledger View
  const plateTotal = document.getElementById("plateTotal");
  const plateExpired = document.getElementById("plateExpired");
  const plateSoon = document.getElementById("plateSoon");
  const plateSafe = document.getElementById("plateSafe");
  const ledgerSearchInput = document.getElementById("ledgerSearchInput");
  const ledgerCategoryFilter = document.getElementById("ledgerCategoryFilter");
  const statusTabs = document.querySelectorAll(".status-tab");
  const refreshLedgerBtn = document.getElementById("refreshLedgerBtn");
  const ledgerTableBody = document.getElementById("ledgerTableBody");
  const ledgerEmptyState = document.getElementById("ledgerEmptyState");

  // DOM Elements - Profile Tab
  const profileForm = document.getElementById("profileForm");
  const profileUsername = document.getElementById("profileUsername");
  const profileDisplayName = document.getElementById("profileDisplayName");
  const profilePassword = document.getElementById("profilePassword");
  const enableNotificationsBtn = document.getElementById("enableNotificationsBtn");
  const notifBtnLabel = document.getElementById("notifBtnLabel");

  // Format today's date in header and date fields
  const today = new Date();
  const todayIso = today.toISOString().split("T")[0];
  const dateOptions = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
  document.getElementById("headerDateDisplay").textContent = today.toLocaleDateString(undefined, dateOptions);
  if (manualDate) manualDate.value = todayIso;
  if (ocrExtractedDate) ocrExtractedDate.value = todayIso;

  // Initialize
  initTheme();
  initScanEngine();
  checkAuthStatus();

  // =========================================================================
  // THEME MANAGEMENT (PARCHMENT LIGHT / NIGHT GRIMOIRE DARK)
  // =========================================================================

  function initTheme() {
    const savedTheme = localStorage.getItem("ledgerTheme") || "light";
    applyTheme(savedTheme);

    themeToggleBtn.addEventListener("click", () => {
      const current = document.documentElement.getAttribute("data-theme") || "light";
      const nextTheme = current === "light" ? "dark" : "light";
      applyTheme(nextTheme);
    });
  }

  function applyTheme(theme) {
    document.documentElement.setAttribute("data-theme", theme);
    localStorage.setItem("ledgerTheme", theme);
    if (themeLabel) {
      themeLabel.textContent = theme === "light" ? "Light" : "Dark";
    }
    const themeMeta = document.querySelector('meta[name="theme-color"]');
    if (themeMeta) {
      themeMeta.setAttribute("content", theme === "light" ? "#f4f1e9" : "#0b0f0d");
    }
  }

  // =========================================================================
  // AUTHENTICATION & PROFILE SETUP
  // =========================================================================

  async function checkAuthStatus() {
    try {
      const res = await fetch("/api/profile");
      const data = await res.json();

      if (!data.configured) {
        // No account exists on server: switch to initial Setup Mode
        setAuthMode(true);
      } else {
        // Account exists: check if user has active session
        const sessionUser = sessionStorage.getItem("ledgerUser");
        if (sessionUser) {
          currentUser = JSON.parse(sessionUser);
          onUserAuthenticated(currentUser);
        } else {
          setAuthMode(false);
        }
      }
    } catch (err) {
      console.warn("Could not reach profile API, using local mode:", err);
      const localUser = localStorage.getItem("localLedgerUser");
      if (localUser) {
        currentUser = JSON.parse(localUser);
        onUserAuthenticated(currentUser);
      } else {
        setAuthMode(true);
      }
    }
  }

  function setAuthMode(setup) {
    isSetupMode = setup;
    if (setup) {
      authBanner.textContent = "Create Your Curator Credentials";
      displayNameField.style.display = "block";
      authSubmitBtn.querySelector(".btn-text").textContent = "Create Account & Enter";
      toggleAuthModeBtn.textContent = "Already have credentials? Sign in";
    } else {
      authBanner.textContent = "Sign In to Open the Ledger";
      displayNameField.style.display = "none";
      authSubmitBtn.querySelector(".btn-text").textContent = "Open Ledger";
      toggleAuthModeBtn.textContent = "Reset or create new credentials";
    }
    authOverlay.classList.remove("hidden");
    bookApp.classList.add("hidden");
  }

  toggleAuthModeBtn.addEventListener("click", () => {
    setAuthMode(!isSetupMode);
  });

  loginForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const username = authUsername.value.trim();
    const password = authPassword.value.trim();
    const displayName = authDisplayName.value.trim();

    if (!username || !password) {
      showToast("Username and password are required.", "error");
      return;
    }

    if (isSetupMode) {
      // Save credentials to server & localStorage
      try {
        const res = await fetch("/api/profile", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username, password, displayName: displayName || username })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "Failed to set up account");

        currentUser = { username: data.username, displayName: data.displayName || data.username };
        sessionStorage.setItem("ledgerUser", JSON.stringify(currentUser));
        localStorage.setItem("localLedgerUser", JSON.stringify(currentUser));
        showToast("Welcome! Credentials inscribed successfully.", "success");
        onUserAuthenticated(currentUser);
      } catch (err) {
        showToast(err.message, "error");
      }
    } else {
      // Authenticate
      try {
        const res = await fetch("/api/login", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ username, password })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || "Invalid username or password");

        currentUser = { username: data.username, displayName: data.displayName || data.username };
        sessionStorage.setItem("ledgerUser", JSON.stringify(currentUser));
        showToast("Ledger unlocked. Welcome back!", "success");
        onUserAuthenticated(currentUser);
      } catch (err) {
        showToast(err.message, "error");
      }
    }
  });

  function onUserAuthenticated(user) {
    authOverlay.classList.add("hidden");
    bookApp.classList.remove("hidden");

    userDisplayName.textContent = user.displayName || user.username;
    const initial = document.getElementById("userInitial");
    if (initial) initial.textContent = (user.displayName || user.username || "L").trim().charAt(0);
    profileUsername.value = user.username;
    profileDisplayName.value = user.displayName || user.username;

    // Load ledger data and trigger login alert
    fetchInventoryItems(true);
  }

  logoutBtn.addEventListener("click", () => {
    sessionStorage.removeItem("ledgerUser");
    currentUser = null;
    authUsername.value = "";
    authPassword.value = "";
    setAuthMode(false);
    showToast("The ledger has been closed and sealed.", "success");
  });

  // Profile Form update
  profileForm.addEventListener("submit", async (e) => {
    e.preventDefault();
    const username = profileUsername.value.trim();
    const displayName = profileDisplayName.value.trim();
    const password = profilePassword.value.trim();

    if (!username || !password) {
      showToast("Username and password cannot be empty.", "error");
      return;
    }

    try {
      const res = await fetch("/api/profile", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password, displayName: displayName || username })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Failed to update profile");

      currentUser = { username: data.username, displayName: data.displayName || data.username };
      sessionStorage.setItem("ledgerUser", JSON.stringify(currentUser));
      userDisplayName.textContent = currentUser.displayName;
      profilePassword.value = "";
      showToast("Profile credentials updated successfully.", "success");
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  // Notifications API permission
  enableNotificationsBtn.addEventListener("click", async () => {
    if (!("Notification" in window)) {
      showToast("Browser does not support notifications.", "error");
      return;
    }
    const perm = await Notification.requestPermission();
    if (perm === "granted") {
      notifBtnLabel.textContent = "Notifications Active";
      showToast("Desktop notification alerts enabled.", "success");
    } else {
      showToast("Notifications not granted by browser.", "error");
    }
  });

  // =========================================================================
  // TOP RIBBON TABS NAVIGATION
  // =========================================================================

  ribbonTabs.forEach(tab => {
    tab.addEventListener("click", () => {
      const targetTab = tab.getAttribute("data-tab");
      switchTab(targetTab);
    });
  });

  function switchTab(tabId) {
    ribbonTabs.forEach(t => {
      const isTarget = t.getAttribute("data-tab") === tabId;
      t.classList.toggle("active", isTarget);
      t.setAttribute("aria-selected", isTarget ? "true" : "false");
    });

    tabContents.forEach(content => {
      content.classList.toggle("active", content.id === `tab-${tabId}`);
    });

    if (tabId === "view") {
      fetchInventoryItems(false);
    }
  }

  // =========================================================================
  // LOGIN EXPIRY ALERT BANNER & NOTIFICATION
  // =========================================================================

  function triggerLoginAlertNotice(items) {
    const alertItems = items.filter(i => i.status === "EXPIRED" || i.status === "EXPIRING_SOON");
    if (alertItems.length === 0) {
      loginAlertBanner.classList.add("hidden");
      return;
    }

    const expiredCount = alertItems.filter(i => i.status === "EXPIRED").length;
    const soonCount = alertItems.length - expiredCount;

    let titleText = "Urgent Expiry Notice";
    let bodyText = "";

    if (expiredCount > 0 && soonCount > 0) {
      bodyText = `${expiredCount} item(s) are already expired, and ${soonCount} will expire within 7 days.`;
    } else if (expiredCount > 0) {
      bodyText = `${expiredCount} item(s) in your ledger have passed their expiration date.`;
    } else {
      bodyText = `${soonCount} item(s) will expire within the next 7 days. Plan to consume them soon.`;
    }

    loginAlertTitle.textContent = titleText;
    loginAlertBody.textContent = bodyText;
    loginAlertBanner.classList.remove("hidden");

    // Send Browser Desktop Notification if enabled
    if ("Notification" in window && Notification.permission === "granted") {
      try {
        new Notification("Expiry Alert Ledger", {
          body: bodyText,
          icon: "data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='%238c2635' stroke-width='2'><path d='M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z'/></svg>"
        });
      } catch (ignored) {}
    }
  }

  viewUrgentLedgerBtn.addEventListener("click", () => {
    switchTab("view");
    statusTabs.forEach(st => {
      if (st.getAttribute("data-status") === "ALERTS") st.click();
    });
    loginAlertBanner.classList.add("hidden");
  });

  dismissAlertBtn.addEventListener("click", () => {
    loginAlertBanner.classList.add("hidden");
  });

  // =========================================================================
  // DATA FETCHING & LEDGER RENDERING
  // =========================================================================

  async function fetchInventoryItems(isInitialLogin = false) {
    try {
      const res = await fetch("/api/items");
      if (!res.ok) throw new Error("HTTP error " + res.status);
      allItems = await res.json();

      updatePlates(allItems);
      populateLedgerCategories(allItems);
      renderLedgerTable();

      if (isInitialLogin) {
        triggerLoginAlertNotice(allItems);
      }
    } catch (err) {
      console.error("Failed to load inventory:", err);
      showToast("Could not load ledger items. Check if server is running.", "error");
    }
  }

  function updatePlates(items) {
    let expired = 0;
    let soon = 0;
    let safe = 0;

    items.forEach(i => {
      if (i.status === "EXPIRED") expired++;
      else if (i.status === "EXPIRING_SOON") soon++;
      else if (i.status === "SAFE") safe++;
    });

    plateTotal.textContent = items.length;
    plateExpired.textContent = expired;
    plateSoon.textContent = soon;
    plateSafe.textContent = safe;
  }

  function populateLedgerCategories(items) {
    const cur = ledgerCategoryFilter.value;
    const cats = Array.from(new Set(items.map(i => i.category).filter(Boolean))).sort();

    ledgerCategoryFilter.innerHTML = '<option value="">All Categories</option>';
    cats.forEach(c => {
      const opt = document.createElement("option");
      opt.value = c;
      opt.textContent = c;
      if (c === cur) opt.selected = true;
      ledgerCategoryFilter.appendChild(opt);
    });
  }

  function renderLedgerTable() {
    const filtered = allItems.filter(item => {
      // Status
      if (currentStatusFilter === "ALERTS") {
        if (item.status !== "EXPIRED" && item.status !== "EXPIRING_SOON") return false;
      } else if (currentStatusFilter !== "ALL") {
        if (item.status !== currentStatusFilter) return false;
      }

      // Category
      if (currentCategory && item.category !== currentCategory) return false;

      // Query
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const mName = item.name.toLowerCase().includes(q);
        const mCat = item.category.toLowerCase().includes(q);
        const mNotes = (item.notes || "").toLowerCase().includes(q);
        if (!mName && !mCat && !mNotes) return false;
      }
      return true;
    });

    ledgerTableBody.innerHTML = "";

    if (filtered.length === 0) {
      ledgerEmptyState.classList.remove("hidden");
      return;
    }
    ledgerEmptyState.classList.add("hidden");

    filtered.forEach(item => {
      const tr = document.createElement("tr");

      let badgeClass = "badge-safe";
      let countClass = "countdown-safe";
      if (item.status === "EXPIRED") {
        badgeClass = "badge-expired";
        countClass = "countdown-expired";
      } else if (item.status === "EXPIRING_SOON") {
        badgeClass = "badge-warning";
        countClass = "countdown-warning";
      }

      tr.dataset.status = item.status;
      tr.style.setProperty(
        "--row-accent",
        item.status === "EXPIRED" ? "var(--danger)"
          : item.status === "EXPIRING_SOON" ? "var(--warn)"
          : "var(--safe)"
      );

      tr.innerHTML = `
        <td class="date-mono cell-id" data-label="No.">#${item.id}</td>
        <td class="cell-name" data-label="Item"><strong>${escapeHtml(item.name)}</strong></td>
        <td data-label="Category"><span class="tag-cat">${escapeHtml(item.category)}</span></td>
        <td data-label="Qty">${item.quantity}</td>
        <td data-label="Expiry"><code class="date-mono">${item.expiryDate}</code></td>
        <td data-label="Time left"><span class="countdown-label ${countClass}">${escapeHtml(item.formattedDays)}</span></td>
        <td data-label="Status"><span class="badge ${badgeClass}">${escapeHtml(item.statusLabel)}</span></td>
        <td data-label="Notes" style="color: var(--ink-muted); font-size: 0.86rem;">${escapeHtml(item.notes || "—")}</td>
        <td class="cell-act" data-label="Action">
          <button class="btn-danger-outline delete-record-btn" data-id="${item.id}" data-name="${escapeHtml(item.name)}" title="Remove item">Delete</button>
        </td>
      `;
      ledgerTableBody.appendChild(tr);
    });

    document.querySelectorAll(".delete-record-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-id");
        const name = btn.getAttribute("data-name");
        deleteLedgerItem(id, name);
      });
    });
  }

  async function deleteLedgerItem(id, name) {
    if (!confirm(`Are you certain you wish to strike out '${name}' (No. ${id}) from the ledger?`)) {
      return;
    }

    try {
      const res = await fetch(`/api/items?id=${id}`, { method: "DELETE" });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Failed to delete item");

      showToast(`Removed '${name}' from the ledger.`, "success");
      fetchInventoryItems(false);
    } catch (err) {
      showToast(err.message, "error");
    }
  }

  // Ledger Filter Listeners
  ledgerSearchInput.addEventListener("input", (e) => {
    searchQuery = e.target.value.trim();
    renderLedgerTable();
  });

  ledgerCategoryFilter.addEventListener("change", (e) => {
    currentCategory = e.target.value;
    renderLedgerTable();
  });

  statusTabs.forEach(st => {
    st.addEventListener("click", () => {
      statusTabs.forEach(s => s.classList.remove("active"));
      st.classList.add("active");
      currentStatusFilter = st.getAttribute("data-status");
      renderLedgerTable();
    });
  });

  refreshLedgerBtn.addEventListener("click", () => {
    fetchInventoryItems(false);
    showToast("Ledger records refreshed.", "success");
  });

  // =========================================================================
  // TAB 2: MANUAL RECORD FORM
  // =========================================================================

  manualAddForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = manualName.value.trim();
    const category = manualCategory.value.trim();
    const quantity = parseInt(manualQuantity.value, 10);
    const expiryDate = manualDate.value;
    const notes = manualNotes.value.trim();

    if (!name || !category || isNaN(quantity) || quantity <= 0 || !expiryDate) {
      showToast("Please provide all required fields.", "error");
      return;
    }

    try {
      const res = await fetch("/api/items", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, category, quantity, expiryDate, notes })
      });
      const created = await res.json();
      if (!res.ok) throw new Error(created.error || "Failed to record item");

      showToast(`Inscribed '${created.name}' successfully into ledger.`, "success");
      manualAddForm.reset();
      manualDate.value = todayIso;
      switchTab("view");
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  // =========================================================================
  // TAB 1: OPTICAL CHARACTER RECOGNITION (OCR) & SMART PARSER
  // =========================================================================

  browsePhotoBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    ocrFileInput.removeAttribute("capture");
    ocrFileInput.click();
  });

  cameraPhotoBtn.addEventListener("click", (e) => {
    e.stopPropagation();
    ocrFileInput.setAttribute("capture", "environment");
    ocrFileInput.click();
  });

  dropZone.addEventListener("click", (e) => {
    if (e.target !== browsePhotoBtn && e.target !== cameraPhotoBtn) {
      ocrFileInput.click();
    }
  });

  dropZone.addEventListener("dragover", (e) => {
    e.preventDefault();
    dropZone.classList.add("drag-over");
  });

  dropZone.addEventListener("dragleave", () => {
    dropZone.classList.remove("drag-over");
  });

  dropZone.addEventListener("drop", (e) => {
    e.preventDefault();
    dropZone.classList.remove("drag-over");
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleSelectedImage(e.dataTransfer.files[0]);
    }
  });

  ocrFileInput.addEventListener("change", (e) => {
    if (e.target.files && e.target.files[0]) {
      handleSelectedImage(e.target.files[0]);
    }
  });

  function handleSelectedImage(file) {
    if (!file.type.startsWith("image/")) {
      showToast("Please select a valid image file.", "error");
      return;
    }

    ocrImageFile = file;
    const reader = new FileReader();
    reader.onload = (evt) => {
      scannedImagePreview.src = evt.target.result;
      dropzoneEmpty.classList.add("hidden");
      previewContainer.classList.remove("hidden");
      scanActionsFooter.classList.remove("hidden");
      ocrProgressCard.classList.add("hidden");
      laserScanner.classList.add("hidden");
      ocrConfidenceBadge.textContent = "Image loaded";
      ocrConfidenceBadge.className = "badge badge-neutral";
    };
    reader.readAsDataURL(file);
  }

  resetScanBtn.addEventListener("click", () => {
    ocrImageFile = null;
    ocrFileInput.value = "";
    previewContainer.classList.add("hidden");
    dropzoneEmpty.classList.remove("hidden");
    scanActionsFooter.classList.add("hidden");
    ocrProgressCard.classList.add("hidden");
    laserScanner.classList.add("hidden");
    ocrAcceptForm.reset();
    ocrExtractedDate.value = todayIso;
    addScannedItemBtn.disabled = true;
    ocrConfidenceBadge.textContent = "Awaiting scan";
    ocrConfidenceBadge.className = "badge badge-neutral";
    rawOcrOutput.textContent = "Upload and decipher a label to view detected text segments.";
  });

  // =========================================================================
  // SCAN ENGINE SWITCHER & GEMINI API KEY SETUP
  // =========================================================================

  // =========================================================================
  // SCAN ENGINE SWITCHER · API KEY STORAGE · GEMINI DIAGNOSTICS
  // =========================================================================

  // Known-good vision models, most preferred first. Live discovery extends this.
  const FALLBACK_MODELS = [
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.0-flash",
    "gemini-2.0-flash-lite",
    "gemini-flash-latest",
    "gemini-1.5-flash-latest"
  ];

  let diagStarted = false;

  function normalizeApiKey(raw) {
    return String(raw || "")
      .trim()
      .replace(/^["'`\s]+|["'`\s]+$/g, "")
      .replace(/^Bearer\s+/i, "")
      .replace(/\s+/g, "");
  }

  function getApiKey() {
    const fromInput = geminiApiKeyInput ? geminiApiKeyInput.value : "";
    return normalizeApiKey(fromInput || localStorage.getItem(KEY_STORAGE) || "");
  }

  function maskKey(key) {
    if (!key) return "(empty)";
    if (key.length <= 12) return key.slice(0, 4) + "…";
    return key.slice(0, 6) + "…" + key.slice(-4);
  }

  function setKeyStatus(state, text) {
    if (!apiKeyStatus) return;
    apiKeyStatus.className = "key-status " + state;
    apiKeyStatus.textContent = text;
  }

  function logDiag(line) {
    if (!aiDiagnostics) return;
    if (!diagStarted) {
      aiDiagnostics.textContent = "";
      diagStarted = true;
    }
    const stamp = new Date().toLocaleTimeString();
    aiDiagnostics.textContent += "[" + stamp + "] " + line + "\n";
    aiDiagnostics.scrollTop = aiDiagnostics.scrollHeight;
  }

  function resetDiag() {
    if (!aiDiagnostics) return;
    aiDiagnostics.textContent = "";
    diagStarted = true;
  }

  function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Turns raw Google errors into something a human can act on.
   * This is the part that was missing: every failure used to surface as a
   * vague "Vision AI error", so a key/quota/model problem looked identical.
   */
  function friendlyGeminiError(status, message) {
    const m = String(message || "");
    if (/API key not valid|API_KEY_INVALID|invalid api key|api key expired/i.test(m)) {
      return "Google rejected the key (API_KEY_INVALID). Copy it again from aistudio.google.com/apikey — no quotes, spaces or line breaks.";
    }
    if (/API has not been used in project|SERVICE_DISABLED|has not been enabled|is disabled/i.test(m)) {
      return "The Generative Language API is turned off for this key's project. Enable it in Google Cloud Console, or create a fresh key in Google AI Studio.";
    }
    if (/PERMISSION_DENIED|not authorized|caller does not have permission|API_KEY_SERVICE_BLOCKED/i.test(m)) {
      return "This key is not permitted to call Gemini (PERMISSION_DENIED). Remove API restrictions on the key, or create an unrestricted key.";
    }
    if (status === 429 || /RESOURCE_EXHAUSTED|quota|rate limit/i.test(m)) {
      return "Free-tier quota for this key is used up. Wait about a minute and retry, or switch to Offline OCR.";
    }
    if (status === 404 || /is not found for API version|not supported for generateContent/i.test(m)) {
      return "This key cannot reach that model (" + m.slice(0, 110) + ").";
    }
    if (status === 503) {
      return "Gemini is busy right now. Try again in a few seconds.";
    }
    if (status === 400) {
      return "Google rejected the request: " + m.slice(0, 240);
    }
    return m.slice(0, 240) || ("HTTP " + status);
  }

  function geminiError(status, message) {
    const err = new Error(friendlyGeminiError(status, message));
    err.status = status;
    err.raw = String(message || "");
    return err;
  }

  function rankModels(names) {
    const preferred = [
      /^gemini-2\.5-flash$/i,
      /^gemini-2\.5-flash-lite$/i,
      /^gemini-flash-latest$/i,
      /^gemini-2\.0-flash$/i,
      /^gemini-2\.0-flash-lite$/i,
      /^gemini-1\.5-flash-latest$/i,
      /^gemini-1\.5-flash$/i
    ];
    const score = name => {
      for (let i = 0; i < preferred.length; i++) {
        if (preferred[i].test(name)) return i;
      }
      if (/flash/i.test(name)) return 20;
      if (/pro/i.test(name)) return 40;
      return 60;
    };
    return Array.from(new Set(names.map(n => String(n).replace(/^models\//, "").trim())))
      .filter(Boolean)
      .sort((a, b) => score(a) - score(b) || a.localeCompare(b));
  }

  async function listVisionModels(key) {
    let res;
    try {
      res = await fetch(
        "https://generativelanguage.googleapis.com/v1beta/models?pageSize=100&key=" + encodeURIComponent(key)
      );
    } catch (netErr) {
      throw new Error("Could not reach Google's API. Check your internet connection (and that the page is opened over http/https, not as a local file).");
    }

    if (!res.ok) {
      const body = await res.text().catch(() => "");
      let message = body;
      try { message = JSON.parse(body).error?.message || body; } catch (ignored) {}
      throw geminiError(res.status, message);
    }

    const data = await res.json().catch(() => ({}));
    const all = Array.isArray(data.models) ? data.models : [];
    logDiag("Model list: " + all.length + " accessible with this key.");

    const usable = all
      .filter(m => {
        const methods = m.supportedGenerationMethods || m.supported_generation_methods || [];
        return Array.isArray(methods)
          && methods.includes("generateContent")
          && /gemini/i.test(m.name || "")
          && !/embedding|aqa|tts|image-generation|imagen/i.test(m.name || "");
      })
      .map(m => m.name);

    return rankModels(usable);
  }

  async function requestGemini(model, key, payload) {
    const url = "https://generativelanguage.googleapis.com/v1beta/models/"
      + encodeURIComponent(model) + ":generateContent?key=" + encodeURIComponent(key);

    let res;
    try {
      res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload)
      });
    } catch (netErr) {
      const err = new Error("Network request to Gemini failed. A browser extension, firewall or offline state may be blocking generativelanguage.googleapis.com.");
      err.status = 0;
      throw err;
    }

    const raw = await res.text().catch(() => "");
    let data = {};
    try { data = raw ? JSON.parse(raw) : {}; } catch (ignored) {}

    if (!res.ok) {
      const message = (data.error && data.error.message) || raw.slice(0, 300) || ("HTTP " + res.status);
      throw geminiError(res.status, message);
    }
    return data;
  }

  function readCandidateText(data) {
    const candidate = data && data.candidates && data.candidates[0];
    if (!candidate) return "";
    const parts = (candidate.content && candidate.content.parts) || [];
    return parts
      .filter(p => p && typeof p.text === "string" && !p.thought)
      .map(p => p.text)
      .join("")
      .trim();
  }

  function readFinishReason(data) {
    return data && data.candidates && data.candidates[0]
      ? data.candidates[0].finishReason
      : undefined;
  }

  function parseJsonLoose(text) {
    let clean = String(text || "").trim();
    clean = clean.replace(/^```(?:json)?/i, "").replace(/```\s*$/, "").trim();

    const candidates = [clean];
    const first = clean.indexOf("{");
    const last = clean.lastIndexOf("}");
    if (first !== -1 && last > first) candidates.push(clean.slice(first, last + 1));

    for (const candidate of candidates) {
      try { return JSON.parse(candidate); } catch (ignored) {}
      try { return JSON.parse(candidate.replace(/,\s*([}\]])/g, "$1")); } catch (ignored) {}
    }

    // Last resort: salvage fields from a reply that was cut off mid-JSON.
    const grab = key => {
      const quoted = clean.match(new RegExp('"' + key + '"\\s*:\\s*"((?:[^"\\\\]|\\\\.)*)"', "i"));
      if (quoted) return quoted[1].replace(/\\"/g, '"').replace(/\\/g, "\\");
      const numeric = clean.match(new RegExp('"' + key + '"\\s*:\\s*(-?\\d+)', "i"));
      return numeric ? numeric[1] : undefined;
    };
    const salvaged = {
      name: grab("name"),
      expiryDate: grab("expiryDate"),
      category: grab("category"),
      quantity: grab("quantity"),
      notes: grab("notes")
    };
    if (salvaged.name || salvaged.expiryDate) return salvaged;
    return null;
  }

  function pad2(value) {
    return String(value).padStart(2, "0");
  }

  function normalizeDate(value) {
    const v = String(value || "").trim();
    let m = v.match(/^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})$/);
    if (m) return m[1] + "-" + pad2(m[2]) + "-" + pad2(m[3]);

    m = v.match(/^(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})$/);
    if (m) return m[3] + "-" + pad2(m[2]) + "-" + pad2(m[1]);

    m = v.match(/^(\d{1,2})[-/.](\d{4})$/);
    if (m) {
      const year = Number(m[2]);
      const month = Number(m[1]);
      const lastDay = new Date(year, month, 0).getDate();
      return year + "-" + pad2(month) + "-" + pad2(lastDay);
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(v)) return v;
    return todayIso;
  }

  function setScanProgress(percent, label) {
    if (ocrProgressBar) ocrProgressBar.style.width = percent + "%";
    if (ocrPercent) ocrPercent.textContent = percent + "%";
    if (ocrStatusText) ocrStatusText.textContent = label;
  }

  function setScanBusy(busy) {
    if (runScanBtn) runScanBtn.disabled = busy;
    if (resetScanBtn) resetScanBtn.disabled = busy;
    if (laserScanner) laserScanner.classList.toggle("hidden", !busy);
    if (busy && ocrProgressCard) ocrProgressCard.classList.remove("hidden");
  }

  function saveApiKeyFromInput(quiet) {
    const key = normalizeApiKey(geminiApiKeyInput ? geminiApiKeyInput.value : "");
    if (!key) {
      setKeyStatus("is-bad", "empty");
      if (!quiet) showToast("Paste your Gemini API key first — use 'Get a free key' above.", "error");
      return false;
    }
    if (key.length < 20 || !/^[A-Za-z0-9._-]+$/.test(key)) {
      setKeyStatus("is-bad", "check key");
      if (!quiet) showToast("That doesn't look like a Gemini key. It is usually ~39 characters and starts with AIza.", "error");
      return false;
    }
    if (geminiApiKeyInput) geminiApiKeyInput.value = key;
    localStorage.setItem(KEY_STORAGE, key);
    setKeyStatus("is-ok", "saved");
    if (!quiet) showToast("Key saved in this browser only. Press Test to verify it with Google.", "success");
    return true;
  }

  async function testGeminiKey() {
    if (!saveApiKeyFromInput(true)) {
      showToast("Add and save an API key first, then press Test.", "error");
      if (geminiApiKeyInput) geminiApiKeyInput.focus();
      return;
    }
    const key = getApiKey();
    resetDiag();
    setKeyStatus("is-busy", "testing…");
    if (testApiKeyBtn) testApiKeyBtn.disabled = true;
    logDiag("Testing key " + maskKey(key) + " (length " + key.length + ")");

    try {
      const models = await listVisionModels(key);
      if (models.length === 0) {
        throw new Error("The key is valid, but no Gemini text/vision model is available for it. Check that the Generative Language API is enabled for the project.");
      }
      logDiag("Models that accept images: " + models.slice(0, 6).join(", "));

      const model = models[0];
      logDiag("Sending a tiny text probe to " + model + " …");
      const probe = await requestGemini(model, key, {
        contents: [{ role: "user", parts: [{ text: "Reply with the single word: READY" }] }],
        generationConfig: { maxOutputTokens: 16, temperature: 0 }
      });
      const reply = readCandidateText(probe);
      logDiag("Probe reply: " + (reply || "(empty)"));

      setKeyStatus("is-ok", "key works");
      showToast("Key works — " + model + " is ready. Scan a label now.", "success");
    } catch (err) {
      logDiag("FAILED: " + err.message);
      setKeyStatus("is-bad", "key failed");
      showToast("Key test failed: " + err.message, "error");
    } finally {
      if (testApiKeyBtn) testApiKeyBtn.disabled = false;
    }
  }

  function initScanEngine() {
    const saved = normalizeApiKey(localStorage.getItem(KEY_STORAGE) || "");
    if (saved && geminiApiKeyInput) {
      geminiApiKeyInput.value = saved;
      setKeyStatus("is-ok", "saved");
    } else {
      setKeyStatus("is-empty", "not set");
    }

    if (engineAiBtn) engineAiBtn.addEventListener("click", () => setScanEngine("ai"));
    if (engineOcrBtn) engineOcrBtn.addEventListener("click", () => setScanEngine("ocr"));
    if (saveApiKeyBtn) saveApiKeyBtn.addEventListener("click", () => saveApiKeyFromInput(false));
    if (testApiKeyBtn) testApiKeyBtn.addEventListener("click", testGeminiKey);

    if (geminiApiKeyInput) {
      geminiApiKeyInput.addEventListener("keydown", e => {
        if (e.key === "Enter") {
          e.preventDefault();
          saveApiKeyFromInput(false);
        }
      });
      geminiApiKeyInput.addEventListener("input", () => {
        const typed = normalizeApiKey(geminiApiKeyInput.value);
        const stored = normalizeApiKey(localStorage.getItem(KEY_STORAGE) || "");
        if (!typed) setKeyStatus("is-empty", "not set");
        else if (typed === stored) setKeyStatus("is-ok", "saved");
        else setKeyStatus("is-empty", "unsaved");
      });
    }

    setScanEngine(activeEngine);
  }

  function setScanEngine(engine) {
    activeEngine = engine;
    localStorage.setItem("scanEngineMode", engine);

    if (engine === "ai") {
      if (engineAiBtn) engineAiBtn.classList.add("active");
      if (engineOcrBtn) engineOcrBtn.classList.remove("active");
      if (aiKeyStrip) aiKeyStrip.classList.remove("hidden");
      if (runScanBtnText) runScanBtnText.textContent = "Read this label";
    } else {
      if (engineOcrBtn) engineOcrBtn.classList.add("active");
      if (engineAiBtn) engineAiBtn.classList.remove("active");
      if (aiKeyStrip) aiKeyStrip.classList.add("hidden");
      if (runScanBtnText) runScanBtnText.textContent = "Read with offline OCR";
    }
  }

  function fileToBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const result = reader.result;
        const commaIdx = result.indexOf(",");
        resolve(commaIdx !== -1 ? result.substring(commaIdx + 1) : result);
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  }

  /**
   * Shrinks a photo before it is sent to Gemini.
   * Phone cameras produce 4-12 MB files, and inline image data is base64
   * inflated by another 33%, which is a common reason scans failed outright.
   * 1600 px on the long edge keeps printed expiry dates crisp.
   */
  async function prepareImageForGemini(file) {
    const fallback = async () => ({
      base64Data: await fileToBase64(file),
      mimeType: file.type || "image/jpeg",
      resized: false
    });

    if (typeof window === "undefined" || !window.createImageBitmap) return fallback();

    try {
      const bitmap = await createImageBitmap(file);
      const maxEdge = 1600;
      const scale = Math.min(1, maxEdge / Math.max(bitmap.width, bitmap.height));
      const width = Math.max(1, Math.round(bitmap.width * scale));
      const height = Math.max(1, Math.round(bitmap.height * scale));

      const canvas = document.createElement("canvas");
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext("2d");
      if (!ctx) {
        if (bitmap.close) bitmap.close();
        return fallback();
      }
      ctx.drawImage(bitmap, 0, 0, width, height);
      if (bitmap.close) bitmap.close();

      const dataUrl = canvas.toDataURL("image/jpeg", 0.88);
      const commaIdx = dataUrl.indexOf(",");
      return {
        base64Data: commaIdx !== -1 ? dataUrl.substring(commaIdx + 1) : dataUrl,
        mimeType: "image/jpeg",
        resized: scale < 1 || file.size > 900 * 1024
      };
    } catch (err) {
      return fallback();
    }
  }

  runScanBtn.addEventListener("click", async () => {
    if (!ocrImageFile) {
      showToast("Please add a label photo first.", "error");
      return;
    }
    if (activeEngine === "ai") {
      await runVisionAiScan();
    } else {
      await runOfflineOcrScan();
    }
  });

  /**
   * High-precision vision scan through Google Gemini.
   *
   * Flow: validate key -> discover models the key can use -> try each model
   * (with a schema-less retry and 429/503 backoff) -> parse JSON -> fill form.
   * Every step is written to the diagnostics log so failures are explainable.
   */
  async function runVisionAiScan() {
    let key = getApiKey();

    if (!key) {
      showToast("No Gemini API key yet. Paste one above (Get a free key) or switch to Offline OCR.", "error");
      if (geminiApiKeyInput) {
        geminiApiKeyInput.focus();
        geminiApiKeyInput.style.outline = "2px solid var(--danger)";
        setTimeout(() => { if (geminiApiKeyInput) geminiApiKeyInput.style.outline = ""; }, 2600);
      }
      return;
    }

    if (saveApiKeyFromInput(true)) key = getApiKey();

    resetDiag();
    setScanBusy(true);
    setKeyStatus("is-busy", "reading");
    if (ocrConfidenceBadge) {
      ocrConfidenceBadge.textContent = "AI reading…";
      ocrConfidenceBadge.className = "badge badge-warning";
    }
    setScanProgress(8, "Preparing image…");
    logDiag("Scan started · " + Math.round((ocrImageFile.size || 0) / 1024) + " KB · " + (ocrImageFile.type || "unknown type"));
    logDiag("Key " + maskKey(key));

    try {
      const prepared = await prepareImageForGemini(ocrImageFile);
      const base64Data = prepared.base64Data;
      const mimeType = prepared.mimeType;
      logDiag("Image ready · " + Math.round((base64Data.length * 0.75) / 1024) + " KB as " + mimeType
        + (prepared.resized ? " (downscaled for upload)" : ""));

      setScanProgress(24, "Checking which models this key can use…");
      let models = [];
      try {
        models = await listVisionModels(key);
      } catch (discoveryErr) {
        logDiag("Model discovery failed: " + discoveryErr.message);
        // A rejected key will fail every model too — stop with the real reason.
        if (discoveryErr.status === 400 || discoveryErr.status === 403) throw discoveryErr;
      }
      if (models.length === 0) {
        models = FALLBACK_MODELS.slice();
        logDiag("Using built-in model list: " + models.join(", "));
      }

      const promptText = [
        "You are a precise packaging label reader for an expiry tracker.",
        "Read the photo and return the product details.",
        "Rules:",
        "- name: the product title exactly as printed, keeping strength/dosage (e.g. 'Amoxicillin 500mg', 'Whole Milk 1L').",
        "- expiryDate: the EXPIRY / BEST BEFORE / USE BY date only, formatted YYYY-MM-DD. Never use the manufacturing or packed date (Mfg / Mfd / Pkd). If only MM/YYYY is printed, use the last day of that month. If no date is legible, return an empty string.",
        "- category: exactly one of Dairy, Bakery, Medicine, Pantry, Poultry, Produce, Meat, Beverages, Household.",
        "- quantity: integer number of units, default 1.",
        "- notes: batch or lot number plus short storage advice."
      ].join("\n");

      const responseSchema = {
        type: "OBJECT",
        properties: {
          name: { type: "STRING" },
          expiryDate: { type: "STRING" },
          category: { type: "STRING" },
          quantity: { type: "INTEGER" },
          notes: { type: "STRING" }
        },
        required: ["name", "expiryDate", "category", "quantity", "notes"]
      };

      /**
       * Builds the request for one model.
       * 2.5 models spend part of maxOutputTokens on internal "thinking", and
       * the old 1024 cap was used up before the JSON arrived — the response
       * came back empty. Thinking is also switched off where it is supported,
       * which makes scans faster and cheaper.
       */
      const buildPayload = (model, withSchema) => ({
        contents: [{
          role: "user",
          parts: [
            { inlineData: { mimeType: mimeType, data: base64Data } },
            { text: promptText }
          ]
        }],
        generationConfig: Object.assign(
          {
            temperature: 0.1,
            maxOutputTokens: 4096,
            responseMimeType: "application/json"
          },
          withSchema ? { responseSchema: responseSchema } : {},
          /2\.5|flash-latest/i.test(model) ? { thinkingConfig: { thinkingBudget: 0 } } : {}
        )
      });

      setScanProgress(46, "Reading the label with Vision AI…");

      let data = null;
      let modelUsed = "";
      let lastError = null;

      for (const model of models.slice(0, 6)) {
        let attemptPayload = buildPayload(model, true);
        for (let attempt = 1; attempt <= 2; attempt++) {
          try {
            logDiag("→ " + model + " (attempt " + attempt + ")");
            data = await requestGemini(model, key, attemptPayload);
            modelUsed = model;
            break;
          } catch (err) {
            lastError = err;
            logDiag("   " + model + " failed — " + err.message);
            if (err.status === 429 || err.status === 503) {
              await sleep(900 * attempt);
              continue;
            }
            if (err.status === 400 && attempt === 1) {
              logDiag("   retrying " + model + " without a response schema…");
              attemptPayload = buildPayload(model, false);
              continue;
            }
            break;
          }
        }
        if (data) break;
      }

      if (!data) {
        throw new Error(lastError ? lastError.message : "No Gemini model accepted the request.");
      }

      setScanProgress(82, "Parsing the extracted fields…");

      const rawText = readCandidateText(data);
      logDiag("Reply from " + modelUsed + ": " + rawText.length + " chars · finish=" + (readFinishReason(data) || "n/a"));

      if (!rawText) {
        const blockReason = data.promptFeedback && data.promptFeedback.blockReason;
        const finish = readFinishReason(data);
        if (blockReason) throw new Error("Gemini blocked this image (" + blockReason + "). Try a clearer, unobstructed photo.");
        if (finish === "MAX_TOKENS") throw new Error("The answer was cut off before any JSON arrived. Try a simpler, tighter photo.");
        throw new Error("Gemini returned an empty answer. Try a sharper photo, or switch to Offline OCR.");
      }

      const parsed = parseJsonLoose(rawText);
      if (!parsed) {
        throw new Error("Gemini replied, but not in JSON. Open 'Raw engine output' to inspect it.");
      }

      const expiry = normalizeDate(parsed.expiryDate);
      const allowedCategories = ["Dairy", "Bakery", "Medicine", "Pantry", "Poultry", "Produce", "Meat", "Beverages", "Household"];
      const rawCategory = String(parsed.category || "").trim();
      const category = allowedCategories.find(c => c.toLowerCase() === rawCategory.toLowerCase()) || "Pantry";
      const quantity = Math.max(1, parseInt(parsed.quantity, 10) || 1);
      const notes = String(parsed.notes || "").trim();

      ocrExtractedName.value = String(parsed.name || "").trim() || "Scanned item";
      ocrExtractedCategory.value = category;
      ocrExtractedQuantity.value = quantity;
      ocrExtractedDate.value = expiry;
      ocrExtractedNotes.value = notes ? notes + " · " + modelUsed : "Read with " + modelUsed;
      addScannedItemBtn.disabled = false;

      ocrConfidenceBadge.textContent = "AI · verified";
      ocrConfidenceBadge.className = "badge badge-safe";
      rawOcrOutput.textContent = JSON.stringify({
        model: modelUsed,
        finishReason: readFinishReason(data) || null,
        parsed: parsed
      }, null, 2);

      setScanProgress(100, "Done — check the fields and save.");
      setKeyStatus("is-ok", "key works");
      logDiag("SUCCESS with " + modelUsed + " · expiry " + expiry);
      showToast("Label read with " + modelUsed + ". Confirm the fields, then save.", "success");

    } catch (err) {
      if (ocrConfidenceBadge) {
        ocrConfidenceBadge.textContent = "Scan failed";
        ocrConfidenceBadge.className = "badge badge-expired";
      }
      if (ocrStatusText) ocrStatusText.textContent = "Scan failed — see diagnostics below";
      if (ocrPercent) ocrPercent.textContent = "—";
      if (ocrProgressBar) ocrProgressBar.style.width = "0%";
      if (aiDiagnostics) {
        logDiag("FAILED: " + err.message);
        const details = aiDiagnostics.closest("details");
        if (details) details.open = true;
      }
      if (rawOcrOutput) rawOcrOutput.textContent = "Vision AI error:\n" + err.message;
      console.error("Vision AI error:", err);
      showToast(err.message, "error");
    } finally {
      setScanBusy(false);
    }
  }

  /**
   * Offline OCR Scan via Tesseract.js with enhanced regex and dot-matrix heuristics.
   */
  async function runOfflineOcrScan() {
    if (typeof Tesseract === "undefined") {
      showToast("Offline OCR library loading failed. Check your internet connection.", "error");
      return;
    }

    laserScanner.classList.remove("hidden");
    ocrProgressCard.classList.remove("hidden");
    runScanBtn.disabled = true;
    ocrConfidenceBadge.textContent = "Deciphering (Offline)...";
    ocrConfidenceBadge.className = "badge badge-warning";

    try {
      const result = await Tesseract.recognize(
        ocrImageFile,
        'eng',
        {
          logger: m => {
            if (m.status === 'recognizing text') {
              const pct = Math.round((m.progress || 0) * 100);
              ocrPercent.textContent = pct + "%";
              ocrProgressBar.style.width = pct + "%";
              ocrStatusText.textContent = `Scanning characters... ${pct}%`;
            } else {
              ocrStatusText.textContent = m.status;
            }
          }
        }
      );

      laserScanner.classList.add("hidden");
      runScanBtn.disabled = false;
      ocrProgressBar.style.width = "100%";
      ocrStatusText.textContent = "Scan complete!";

      const text = result.data.text || "";
      rawOcrOutput.textContent = text.trim() || "[No readable text characters detected]";

      parseOcrResults(text, result.data.confidence);
    } catch (err) {
      laserScanner.classList.add("hidden");
      runScanBtn.disabled = false;
      console.error("OCR recognition error:", err);
      showToast("Error processing image characters: " + err.message, "error");
      ocrConfidenceBadge.textContent = "Scan failed";
      ocrConfidenceBadge.className = "badge badge-expired";
    }
  }

  /**
   * Intelligently parses product name, expiry date, and category from raw OCR text.
   */
  function parseOcrResults(text, confidence) {
    ocrConfidenceBadge.textContent = `Confidence: ${Math.round(confidence)}%`;
    ocrConfidenceBadge.className = confidence > 70 ? "badge badge-safe" : "badge badge-warning";

    const lines = text.split("\n").map(l => l.trim()).filter(l => l.length > 1);

    // 1. EXTRACT EXPIRY DATE WITH DOT-MATRIX CORRECTIONS
    let detectedDate = extractExpiryDateFromText(text);
    if (!detectedDate) {
      detectedDate = todayIso;
    }
    ocrExtractedDate.value = detectedDate;

    // 2. EXTRACT PRODUCT NAME
    let detectedName = extractProductNameFromLines(lines);
    ocrExtractedName.value = detectedName || "Scanned Perishable Item";

    // 3. DETECT CATEGORY
    let detectedCategory = detectCategoryFromText(text);
    ocrExtractedCategory.value = detectedCategory;

    ocrExtractedQuantity.value = 1;
    ocrExtractedNotes.value = `Scanned via Offline OCR (${Math.round(confidence)}% match)`;
    addScannedItemBtn.disabled = false;

    showToast("Characters deciphered! Check the details and record.", "success");
  }

  /**
   * Enhanced Expiry Date Search with Packaging Line Heuristics and Dot-Matrix Correction.
   */
  function extractExpiryDateFromText(text) {
    if (!text) return null;
    const lines = text.split("\n").map(l => l.trim()).filter(l => l.length > 0);

    const expiryKeywords = /exp|expiry|best before|use by|bb|validity|val/i;
    const mfgKeywords = /mfg|mfd|pkd|packed|batch/i;

    // Prioritize lines with explicit expiry cues over general lines
    const targetLines = lines.filter(l => expiryKeywords.test(l) && !mfgKeywords.test(l));
    const candidateLines = targetLines.length > 0 ? targetLines : lines.filter(l => !mfgKeywords.test(l));

    for (const line of (candidateLines.length > 0 ? candidateLines : lines)) {
      // 1. DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY
      const dmyRegex = /\b(0[1-9]|[12]\d|3[01])[-/.](0[1-9]|1[0-2])[-/.](20\d{2})\b/;
      let m = line.match(dmyRegex);
      if (m) {
        let year = m[3];
        // Dot-matrix correction: if OCR read 205x (e.g. 2054 instead of 2024), correct to 202x
        if (year.startsWith("205") && parseInt(year, 10) > 2040) {
          year = "202" + year.substring(3);
        }
        return `${year}-${m[2]}-${m[1]}`;
      }

      // 2. YYYY-MM-DD or YYYY/MM/DD
      const isoRegex = /\b(20\d{2})[-/.](0[1-9]|1[0-2])[-/.](0[1-9]|[12]\d|3[01])\b/;
      m = line.match(isoRegex);
      if (m) {
        let year = m[1];
        if (year.startsWith("205") && parseInt(year, 10) > 2040) {
          year = "202" + year.substring(3);
        }
        return `${year}-${m[2]}-${m[3]}`;
      }

      // 3. DD/MM/YY
      const dmyShortRegex = /\b(0[1-9]|[12]\d|3[01])[-/.](0[1-9]|1[0-2])[-/.](2[4-9]|3[0-9])\b/;
      m = line.match(dmyShortRegex);
      if (m) {
        return `20${m[3]}-${m[2]}-${m[1]}`;
      }

      // 4. MM/YYYY (e.g. 05/2024)
      const myRegex = /\b(0[1-9]|1[0-2])[-/.](20\d{2})\b/;
      m = line.match(myRegex);
      if (m) {
        let year = m[2];
        if (year.startsWith("205") && parseInt(year, 10) > 2040) {
          year = "202" + year.substring(3);
        }
        return `${year}-${m[1]}-28`;
      }

      // 5. English month words (e.g. 15 Oct 2026, EXP MAY 2024)
      const months = {
        jan: "01", feb: "02", mar: "03", apr: "04", may: "05", jun: "06",
        jul: "07", aug: "08", sep: "09", oct: "10", nov: "11", dec: "12"
      };
      const wordDateRegex = /\b(0[1-9]|[12]\d|3[01])\s*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s*(20\d{2}|2[4-9])\b/i;
      m = line.match(wordDateRegex);
      if (m) {
        const d = m[1];
        const mon = months[m[2].toLowerCase().substring(0, 3)];
        let y = m[3].length === 2 ? `20${m[3]}` : m[3];
        if (y.startsWith("205") && parseInt(y, 10) > 2040) {
          y = "202" + y.substring(3);
        }
        return `${y}-${mon}-${d}`;
      }
    }

    return null;
  }

  /**
   * Filters out legal/nutrition boilerplate to find the product title.
   */
  function extractProductNameFromLines(lines) {
    const ignoreList = [
      "ingredients", "nutrition", "calories", "protein", "fat", "net wt", "best before",
      "expiry", "exp date", "mfg", "batch", "keep refrigerated", "store in", "contains",
      "barcode", "manufactured", "marketed by", "fssai", "usda", "gmo", "alamy", "shutterstock"
    ];

    for (const line of lines) {
      const lower = line.toLowerCase();
      const hasIgnored = ignoreList.some(k => lower.includes(k));
      const letters = (line.match(/[a-zA-Z]/g) || []).length;
      // Skip lines that are mostly garbage or watermarks
      if (!hasIgnored && letters >= 4 && line.length < 50 && !/^v\d\s+cat/i.test(lower)) {
        return line.replace(/[^\w\s-&]/g, "").trim();
      }
    }
    return lines.length > 0 ? lines[0].substring(0, 40) : "";
  }

  /**
   * Guesses category from keywords.
   */
  function detectCategoryFromText(text) {
    const lower = text.toLowerCase();
    if (/milk|cheese|yogurt|butter|cream|dairy|paneer|curd/i.test(lower)) return "Dairy";
    if (/bread|toast|bakery|cookie|biscuit|cake|sourdough|flour|bun/i.test(lower)) return "Bakery";
    if (/tablet|capsule|syrup|paracetamol|aspirin|mg|pharma|medicine|vitamin|ointment/i.test(lower)) return "Medicine";
    if (/oil|vinegar|sauce|rice|pasta|spice|salt|sugar|canned|beans|flour|cereal|grain/i.test(lower)) return "Pantry";
    if (/apple|banana|orange|tomato|fruit|vegetable|produce|salad|berry|onion/i.test(lower)) return "Produce";
    if (/egg|chicken|poultry/i.test(lower)) return "Poultry";
    if (/beef|pork|mutton|meat|fish|salmon/i.test(lower)) return "Meat";
    if (/juice|water|tea|coffee|soda|beverage|drink|beer|wine/i.test(lower)) return "Beverages";
    return "Pantry";
  }

  // Accept OCR item form submit
  ocrAcceptForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = ocrExtractedName.value.trim();
    const category = ocrExtractedCategory.value.trim();
    const quantity = parseInt(ocrExtractedQuantity.value, 10);
    const expiryDate = ocrExtractedDate.value;
    const notes = ocrExtractedNotes.value.trim();

    if (!name || !category || isNaN(quantity) || quantity <= 0 || !expiryDate) {
      showToast("Please check all fields before recording.", "error");
      return;
    }

    try {
      const res = await fetch("/api/items", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, category, quantity, expiryDate, notes })
      });
      const created = await res.json();
      if (!res.ok) throw new Error(created.error || "Failed to record item");

      showToast(`Inscribed '${created.name}' successfully into ledger!`, "success");
      resetScanBtn.click();
      switchTab("view");
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  // =========================================================================
  // TOAST NOTIFICATION UTILITY
  // =========================================================================

  function showToast(message, type = "success") {
    const shelf = document.getElementById("toastShelf");
    const toast = document.createElement("div");
    toast.className = `toast-paper toast-${type}`;

    // SVG icon for toast
    const iconSvg = type === "success" 
      ? `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>`
      : `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`;

    toast.innerHTML = `${iconSvg}<span>${escapeHtml(message)}</span>`;
    shelf.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transform = "translateY(10px)";
      toast.style.transition = "all 0.3s ease";
      setTimeout(() => toast.remove(), 320);
    }, 3500);
  }

  function escapeHtml(str) {
    if (!str) return "";
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#039;");
  }

});

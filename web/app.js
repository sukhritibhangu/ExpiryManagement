/**
 * Expiry Alert Ledger - Master Application Controller
 * 
 * Features:
 * - Simple Custom Username & Password Authentication & Profile Management
 * - Optical Character Recognition (OCR) using Tesseract.js with Laser Scanner
 * - Expiry Date & Product Name Extraction Engine
 * - Digital Book Theme with Dark & Light Mode Switcher
 * - Top Ribbon Tab Navigation
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
      themeLabel.textContent = theme === "light" ? "Parchment" : "Grimoire";
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

      tr.innerHTML = `
        <td class="date-mono">#${item.id}</td>
        <td><strong>${escapeHtml(item.name)}</strong></td>
        <td><span class="tag-cat">${escapeHtml(item.category)}</span></td>
        <td>${item.quantity}</td>
        <td><code class="date-mono">${item.expiryDate}</code></td>
        <td><span class="countdown-label ${countClass}">${escapeHtml(item.formattedDays)}</span></td>
        <td><span class="badge ${badgeClass}">${escapeHtml(item.statusLabel)}</span></td>
        <td style="color: var(--ink-muted); font-size: 0.88rem;">${escapeHtml(item.notes || "—")}</td>
        <td style="text-align: center;">
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

  runScanBtn.addEventListener("click", async () => {
    if (!ocrImageFile) {
      showToast("Please upload an image first.", "error");
      return;
    }

    // Check if Tesseract.js is loaded
    if (typeof Tesseract === "undefined") {
      showToast("OCR engine loading failed. Please check internet connection.", "error");
      return;
    }

    laserScanner.classList.remove("hidden");
    ocrProgressCard.classList.remove("hidden");
    runScanBtn.disabled = true;
    ocrConfidenceBadge.textContent = "Deciphering...";
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
  });

  /**
   * Intelligently parses product name, expiry date, and category from raw OCR text.
   */
  function parseOcrResults(text, confidence) {
    ocrConfidenceBadge.textContent = `Confidence: ${Math.round(confidence)}%`;
    ocrConfidenceBadge.className = confidence > 70 ? "badge badge-safe" : "badge badge-warning";

    const lines = text.split("\n").map(l => l.trim()).filter(l => l.length > 1);

    // 1. EXTRACT EXPIRY DATE
    let detectedDate = extractExpiryDateFromText(text);
    if (!detectedDate) {
      // Default fallback: 1 month from now or today
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
    ocrExtractedNotes.value = `Scanned via OCR (${Math.round(confidence)}% match)`;
    addScannedItemBtn.disabled = false;

    showToast("Characters deciphered! Check the details and record.", "success");
  }

  /**
   * Searches OCR text for expiry patterns.
   */
  function extractExpiryDateFromText(text) {
    // Common date patterns
    // YYYY-MM-DD or YYYY/MM/DD
    const isoRegex = /\b(20\d{2})[-/.](0[1-9]|1[0-2])[-/.](0[1-9]|[12]\d|3[01])\b/;
    let m = text.match(isoRegex);
    if (m) {
      return `${m[1]}-${m[2]}-${m[3]}`;
    }

    // DD/MM/YYYY or DD-MM-YYYY or DD.MM.YYYY
    const dmyRegex = /\b(0[1-9]|[12]\d|3[01])[-/.](0[1-9]|1[0-2])[-/.](20\d{2})\b/;
    m = text.match(dmyRegex);
    if (m) {
      return `${m[3]}-${m[2]}-${m[1]}`;
    }

    // DD/MM/YY
    const dmyShortRegex = /\b(0[1-9]|[12]\d|3[01])[-/.](0[1-9]|1[0-2])[-/.](2[4-9]|3[0-9])\b/;
    m = text.match(dmyShortRegex);
    if (m) {
      return `20${m[3]}-${m[2]}-${m[1]}`;
    }

    // MM/YYYY (e.g. 10/2026) -> convert to end of month
    const myRegex = /\b(0[1-9]|1[0-2])[-/.](20\d{2})\b/;
    m = text.match(myRegex);
    if (m) {
      return `${m[2]}-${m[1]}-28`;
    }

    // English month words: e.g. 15 Oct 2026, EXP 22 DEC 2026
    const months = {
      jan: "01", feb: "02", mar: "03", apr: "04", may: "05", jun: "06",
      jul: "07", aug: "08", sep: "09", oct: "10", nov: "11", dec: "12"
    };
    const wordDateRegex = /\b(0[1-9]|[12]\d|3[01])\s*(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\s*(20\d{2}|2[4-9])\b/i;
    m = text.match(wordDateRegex);
    if (m) {
      const d = m[1];
      const mon = months[m[2].toLowerCase().substring(0, 3)];
      const y = m[3].length === 2 ? `20${m[3]}` : m[3];
      return `${y}-${mon}-${d}`;
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
      "barcode", "manufactured", "marketed by", "fssai", "usda", "gmo"
    ];

    for (const line of lines) {
      const lower = line.toLowerCase();
      const hasIgnored = ignoreList.some(k => lower.includes(k));
      // Good line has letters, isn't mostly digits, and isn't boilerplate
      const letters = (line.match(/[a-zA-Z]/g) || []).length;
      if (!hasIgnored && letters >= 4 && line.length < 50) {
        // Clean line
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

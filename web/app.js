/**
 * Expiry Alert System - Frontend Single Page Application
 * 
 * Interacts with the Core Java WebServer REST API (/api/items, /api/alerts).
 * Provides live instant filtering, search, modal dialogs, and alerts.
 */

document.addEventListener("DOMContentLoaded", () => {
  let allItems = [];
  let currentStatusFilter = "ALL";
  let currentCategory = "";
  let searchQuery = "";

  // DOM Elements
  const inventoryTableBody = document.getElementById("inventoryTableBody");
  const emptyState = document.getElementById("emptyState");
  const recordCount = document.getElementById("recordCount");

  // Metrics
  const statTotal = document.getElementById("statTotal");
  const statExpired = document.getElementById("statExpired");
  const statSoon = document.getElementById("statSoon");
  const statSafe = document.getElementById("statSafe");

  // Alert Banner
  const alertBanner = document.getElementById("alertBanner");
  const alertBannerTitle = document.getElementById("alertBannerTitle");
  const alertBannerMsg = document.getElementById("alertBannerMsg");
  const filterAlertsBtn = document.getElementById("filterAlertsBtn");

  // Controls
  const searchInput = document.getElementById("searchInput");
  const categoryFilter = document.getElementById("categoryFilter");
  const tabButtons = document.querySelectorAll(".tab-btn");
  const refreshBtn = document.getElementById("refreshBtn");
  const currentDatePill = document.getElementById("currentDatePill");

  // Modal
  const addModal = document.getElementById("addModal");
  const openAddModalBtn = document.getElementById("openAddModalBtn");
  const closeModalBtn = document.getElementById("closeModalBtn");
  const cancelModalBtn = document.getElementById("cancelModalBtn");
  const addItemForm = document.getElementById("addItemForm");
  const itemExpiryDateInput = document.getElementById("itemExpiryDate");

  // Format today's date in header and set default date picker
  const today = new Date();
  const todayStr = today.toISOString().split("T")[0];
  if (currentDatePill) {
    const options = { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' };
    currentDatePill.textContent = "📅 Today: " + today.toLocaleDateString(undefined, options);
  }
  if (itemExpiryDateInput) {
    itemExpiryDateInput.value = todayStr;
  }

  // Initial Load
  fetchItems();

  // =========================================================================
  // DATA FETCHING & RENDERING
  // =========================================================================

  async function fetchItems() {
    try {
      const res = await fetch("/api/items");
      if (!res.ok) throw new Error("Server responded with " + res.status);
      allItems = await res.json();

      updateMetrics(allItems);
      updateAlertBanner(allItems);
      populateCategoryOptions(allItems);
      renderTable();
    } catch (err) {
      console.error("Failed to load inventory items:", err);
      showToast("Error loading inventory items. Is the Java server running?", "error");
    }
  }

  function updateMetrics(items) {
    let expired = 0;
    let soon = 0;
    let safe = 0;

    items.forEach(item => {
      if (item.status === "EXPIRED") expired++;
      else if (item.status === "EXPIRING_SOON") soon++;
      else if (item.status === "SAFE") safe++;
    });

    statTotal.textContent = items.length;
    statExpired.textContent = expired;
    statSoon.textContent = soon;
    statSafe.textContent = safe;
  }

  function updateAlertBanner(items) {
    const alertItems = items.filter(i => i.status === "EXPIRED" || i.status === "EXPIRING_SOON");

    if (alertItems.length === 0) {
      alertBanner.classList.add("hidden");
      return;
    }

    const expiredCount = alertItems.filter(i => i.status === "EXPIRED").length;
    const soonCount = alertItems.length - expiredCount;

    let msg = "";
    if (expiredCount > 0 && soonCount > 0) {
      msg = `${expiredCount} item(s) are already expired, and ${soonCount} item(s) will expire within the next 7 days!`;
    } else if (expiredCount > 0) {
      msg = `${expiredCount} item(s) in your inventory have expired!`;
    } else {
      msg = `${soonCount} item(s) will expire within the next 7 days. Plan to use them soon!`;
    }

    alertBannerTitle.textContent = `⚠️ Automatic Expiry Alert: ${alertItems.length} item(s) need attention`;
    alertBannerMsg.textContent = msg;
    alertBanner.classList.remove("hidden");
  }

  function populateCategoryOptions(items) {
    const currentVal = categoryFilter.value;
    const categories = Array.from(new Set(items.map(i => i.category).filter(Boolean))).sort();

    categoryFilter.innerHTML = '<option value="">All Categories</option>';
    categories.forEach(cat => {
      const opt = document.createElement("option");
      opt.value = cat;
      opt.textContent = cat;
      if (cat === currentVal) opt.selected = true;
      categoryFilter.appendChild(opt);
    });
  }

  function renderTable() {
    const filtered = allItems.filter(item => {
      // 1. Status Filter
      if (currentStatusFilter === "ALERTS") {
        if (item.status !== "EXPIRED" && item.status !== "EXPIRING_SOON") return false;
      } else if (currentStatusFilter !== "ALL") {
        if (item.status !== currentStatusFilter) return false;
      }

      // 2. Category Filter
      if (currentCategory && item.category !== currentCategory) {
        return false;
      }

      // 3. Search Query
      if (searchQuery) {
        const q = searchQuery.toLowerCase();
        const matchesName = item.name.toLowerCase().includes(q);
        const matchesCat = item.category.toLowerCase().includes(q);
        const matchesNotes = (item.notes || "").toLowerCase().includes(q);
        if (!matchesName && !matchesCat && !matchesNotes) return false;
      }

      return true;
    });

    inventoryTableBody.innerHTML = "";
    recordCount.textContent = `Showing ${filtered.length} of ${allItems.length} item(s)`;

    if (filtered.length === 0) {
      emptyState.classList.remove("hidden");
      return;
    }
    emptyState.classList.add("hidden");

    filtered.forEach(item => {
      const tr = document.createElement("tr");

      // Badge class
      let badgeClass = "badge-safe";
      let countdownClass = "countdown-safe";
      if (item.status === "EXPIRED") {
        badgeClass = "badge-expired";
        countdownClass = "countdown-expired";
      } else if (item.status === "EXPIRING_SOON") {
        badgeClass = "badge-warning";
        countdownClass = "countdown-warning";
      }

      tr.innerHTML = `
        <td><strong>#${item.id}</strong></td>
        <td><strong>${escapeHtml(item.name)}</strong></td>
        <td><span class="tag-category">${escapeHtml(item.category)}</span></td>
        <td>${item.quantity}</td>
        <td><code>${item.expiryDate}</code></td>
        <td><span class="countdown-text ${countdownClass}">${escapeHtml(item.formattedDays)}</span></td>
        <td><span class="badge ${badgeClass}">${escapeHtml(item.statusLabel)}</span></td>
        <td style="color: var(--text-muted); font-size: 0.85rem;">${escapeHtml(item.notes || "—")}</td>
        <td style="text-align: center;">
          <button class="btn btn-danger-outline delete-btn" data-id="${item.id}" data-name="${escapeHtml(item.name)}">Delete</button>
        </td>
      `;

      inventoryTableBody.appendChild(tr);
    });

    // Attach delete listeners
    document.querySelectorAll(".delete-btn").forEach(btn => {
      btn.addEventListener("click", () => {
        const id = btn.getAttribute("data-id");
        const name = btn.getAttribute("data-name");
        handleDeleteItem(id, name);
      });
    });
  }

  // =========================================================================
  // ACTIONS: ADD & DELETE
  // =========================================================================

  addItemForm.addEventListener("submit", async (e) => {
    e.preventDefault();

    const name = document.getElementById("itemName").value.trim();
    const category = document.getElementById("itemCategory").value.trim();
    const quantity = parseInt(document.getElementById("itemQuantity").value, 10);
    const expiryDate = document.getElementById("itemExpiryDate").value;
    const notes = document.getElementById("itemNotes").value.trim();

    if (!name || !category || isNaN(quantity) || quantity <= 0 || !expiryDate) {
      showToast("Please fill in all required fields properly.", "error");
      return;
    }

    try {
      const res = await fetch("/api/items", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, category, quantity, expiryDate, notes })
      });

      if (!res.ok) {
        const errJson = await res.json();
        throw new Error(errJson.error || "Failed to add item");
      }

      const created = await res.json();
      showToast(`Added '${created.name}' successfully!`, "success");

      // Reset form & close modal
      addItemForm.reset();
      itemExpiryDateInput.value = todayStr;
      addModal.close();

      // Refresh data
      await fetchItems();
    } catch (err) {
      showToast(err.message, "error");
    }
  });

  async function handleDeleteItem(id, name) {
    if (!confirm(`Are you sure you want to delete '${name}' (ID: ${id})?`)) {
      return;
    }

    try {
      const res = await fetch(`/api/items?id=${id}`, { method: "DELETE" });
      if (!res.ok) {
        const errJson = await res.json();
        throw new Error(errJson.error || "Failed to delete item");
      }

      showToast(`Deleted '${name}' (#${id}) successfully.`, "success");
      await fetchItems();
    } catch (err) {
      showToast(err.message, "error");
    }
  }

  // =========================================================================
  // EVENT LISTENERS
  // =========================================================================

  // Search input
  searchInput.addEventListener("input", (e) => {
    searchQuery = e.target.value.trim();
    renderTable();
  });

  // Category dropdown filter
  categoryFilter.addEventListener("change", (e) => {
    currentCategory = e.target.value;
    renderTable();
  });

  // Status Tab Buttons
  tabButtons.forEach(btn => {
    btn.addEventListener("click", () => {
      tabButtons.forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      currentStatusFilter = btn.getAttribute("data-status");
      renderTable();
    });
  });

  // Alert banner button
  filterAlertsBtn.addEventListener("click", () => {
    tabButtons.forEach(b => {
      if (b.getAttribute("data-status") === "ALERTS") {
        b.click();
      }
    });
    document.querySelector(".table-card").scrollIntoView({ behavior: "smooth" });
  });

  // Refresh button
  refreshBtn.addEventListener("click", () => {
    fetchItems();
    showToast("Inventory refreshed.", "success");
  });

  // Modal open / close
  openAddModalBtn.addEventListener("click", () => {
    addModal.showModal();
    document.getElementById("itemName").focus();
  });

  closeModalBtn.addEventListener("click", () => addModal.close());
  cancelModalBtn.addEventListener("click", () => addModal.close());

  // Close modal when clicking outside box
  addModal.addEventListener("click", (e) => {
    const dialogDimensions = addModal.getBoundingClientRect();
    if (
      e.clientX < dialogDimensions.left ||
      e.clientX > dialogDimensions.right ||
      e.clientY < dialogDimensions.top ||
      e.clientY > dialogDimensions.bottom
    ) {
      addModal.close();
    }
  });

  // Toast Helper
  function showToast(message, type = "success") {
    const toastContainer = document.getElementById("toastContainer");
    const toast = document.createElement("div");
    toast.className = `toast toast-${type}`;
    toast.textContent = (type === "success" ? "✅ " : "⚠️ ") + message;

    toastContainer.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = "0";
      toast.style.transition = "opacity 0.3s ease";
      setTimeout(() => toast.remove(), 300);
    }, 3200);
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

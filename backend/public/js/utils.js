(() => {
  "use strict";

  const TOKEN_KEY = "va_pilot_auth_token";
  const USER_KEY = "va_pilot_current_user";

  function qs(selector, root = document) {
    return root.querySelector(selector);
  }

  function pickMessageTarget(type, targetId) {
    if (targetId) return document.getElementById(targetId);
    if (type === "success") {
      return (
        document.getElementById("success-msg") ||
        document.getElementById("message") ||
        document.querySelector("[data-message]")
      );
    }
    return (
      document.getElementById("error-msg") ||
      document.getElementById("message") ||
      document.querySelector("[data-message]")
    );
  }

  function showMessage(message, type = "error", options = {}) {
    const target = pickMessageTarget(type, options.targetId);
    if (!target) {
      if (options.fallbackAlert !== false) alert(message);
      return;
    }

    target.textContent = message;
    target.classList.remove("hidden");

    if (type === "success") {
      target.classList.remove("text-red-500");
      target.classList.add("text-green-500");
    } else {
      target.classList.remove("text-green-500");
      target.classList.add("text-red-500");
    }

    if (options.autoHideMs) {
      setTimeout(() => target.classList.add("hidden"), options.autoHideMs);
    }
  }

  function showError(message, options) {
    showMessage(message, "error", options);
  }

  function showSuccess(message, options) {
    showMessage(message, "success", options);
  }

  function saveAuthToken(token) {
    if (!token) return;
    localStorage.setItem(TOKEN_KEY, token);
  }

  function getAuthToken() {
    return localStorage.getItem(TOKEN_KEY) || "";
  }

  function clearAuthToken() {
    localStorage.removeItem(TOKEN_KEY);
  }

  function saveCurrentUser(user) {
    if (!user) return;
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }

  function getCurrentUser() {
    try {
      return JSON.parse(localStorage.getItem(USER_KEY) || "null");
    } catch (e) {
      return null;
    }
  }

  function clearCurrentUser() {
    localStorage.removeItem(USER_KEY);
  }

  function isLoggedIn() {
    return Boolean(getAuthToken() || getCurrentUser());
  }

  window.VA = window.VA || {};
  window.VA.utils = {
    qs,
    showMessage,
    showError,
    showSuccess,
    saveAuthToken,
    getAuthToken,
    clearAuthToken,
    saveCurrentUser,
    getCurrentUser,
    clearCurrentUser,
    isLoggedIn
  };
})();

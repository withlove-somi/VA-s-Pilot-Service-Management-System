(() => {
  "use strict";

  const BASE_URL =
    window.API_BASE ||
    window.__API_BASE__ ||
    "https://va-s-pilot-service-management-system-production-d6a0.up.railway.app";

  async function apiRequest(path, options = {}) {
    const url = path.startsWith("http") ? path : `${BASE_URL}${path}`;
    const headers = {
      "Content-Type": "application/json",
      ...(options.headers || {})
    };

    const token = window.VA?.utils?.getAuthToken?.();
    if (token) headers.Authorization = `Bearer ${token}`;

    let response;
    try {
      response = await fetch(url, {
        ...options,
        headers
      });
    } catch (err) {
      const error = new Error("Network error. Please check your connection.");
      error.cause = err;
      throw error;
    }

    let data = null;
    const text = await response.text();
    try {
      data = text ? JSON.parse(text) : null;
    } catch (err) {
      data = text ? { raw: text } : null;
    }

    if (!response.ok || (data && data.ok === false)) {
      const message = data?.error || data?.message || `Request failed (${response.status})`;
      const error = new Error(message);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  }

  function healthCheck() {
    return apiRequest("/api/health");
  }

  function login(email, password) {
    return apiRequest("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });
  }

  function register(fullname, email, password) {
    return apiRequest("/api/auth/register", {
      method: "POST",
      body: JSON.stringify({ fullname, email, password })
    });
  }

  function getUserByEmail(email) {
    return apiRequest(`/api/users/${encodeURIComponent(email)}`);
  }

  window.VA = window.VA || {};
  window.VA.api = {
    BASE_URL,
    apiRequest,
    healthCheck,
    login,
    register,
    getUserByEmail
  };
})();

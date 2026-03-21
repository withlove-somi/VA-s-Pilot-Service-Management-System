(() => {
  "use strict";

  const utils = window.VA?.utils;
  const api = window.VA?.api;

  async function runHealthCheck() {
    if (!api?.healthCheck) return;
    try {
      const health = await api.healthCheck();
      console.info("Backend health OK", health);
    } catch (err) {
      console.warn("Backend health check failed", err);
      utils?.showError?.("Unable to reach the server right now.", { autoHideMs: 4000 });
    }
  }

  function markAuthState() {
    if (!utils?.isLoggedIn) return;
    if (utils.isLoggedIn()) {
      document.body.classList.add("is-logged-in");
      document.body.classList.remove("is-logged-out");
    } else {
      document.body.classList.add("is-logged-out");
      document.body.classList.remove("is-logged-in");
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    markAuthState();
    runHealthCheck();
  });
})();

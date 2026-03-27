(() => {
  "use strict";

  const DEFAULT_API_BASE =
    "https://va-s-pilot-service-management-system-production-d6a0.up.railway.app";

  const fromWindow = window.API_BASE || window.__API_BASE__;
  const fromBody = document.body?.dataset?.apiBase;
  const fromMeta = document.querySelector('meta[name="api-base"]')?.content;

  const candidate = String(fromWindow || fromBody || fromMeta || DEFAULT_API_BASE).trim();
  const apiBase = candidate.replace(/\/+$/, "");

  window.API_BASE = apiBase;
  window.__API_BASE__ = apiBase;

  window.VA = window.VA || {};
  window.VA.config = Object.assign(window.VA.config || {}, { API_BASE: apiBase });
})();

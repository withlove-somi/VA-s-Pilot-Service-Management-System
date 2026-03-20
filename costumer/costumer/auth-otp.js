const VA_PILOT_OTP_KEY = "va_pilot_pending_otp";
const VA_PILOT_EMAILJS_CONFIG_KEY = "va_pilot_emailjs_config";
const VA_PILOT_OTP_TTL_MS = 5 * 60 * 1000;

window.VA_EMAILJS_CONFIG = window.VA_EMAILJS_CONFIG || {
  publicKey: "SgkQyK2k1yc7lGO_R",
  serviceId: "service_8j4tgje",
  templateId: "template_b4qhvvg"
};

function getEmailJsConfig() {
  const configured = JSON.parse(localStorage.getItem(VA_PILOT_EMAILJS_CONFIG_KEY) || "null");
  const fallback = window.VA_EMAILJS_CONFIG || {};
  return {
    publicKey: configured?.publicKey || fallback.publicKey || "",
    serviceId: configured?.serviceId || fallback.serviceId || "",
    templateId: configured?.templateId || fallback.templateId || ""
  };
}

function ensureEmailJsReady() {
  if (!window.emailjs) {
    throw new Error("Email service is not loaded. Please refresh and try again.");
  }

  const config = getEmailJsConfig();
  if (!config.publicKey || !config.serviceId || !config.templateId) {
    throw new Error("EmailJS config missing. Set publicKey, serviceId, and templateId.");
  }

  // Common config mistake: using public key as template id.
  if (config.templateId === config.publicKey) {
    throw new Error("EmailJS templateId is invalid. Use your EmailJS template ID (example: template_xxxxxxx).");
  }

  emailjs.init({ publicKey: config.publicKey });
  return config;
}

function generateOtpCode() {
  return String(Math.floor(100000 + Math.random() * 900000));
}

function maskEmail(email) {
  const [name, domain] = (email || "").split("@");
  if (!name || !domain) return email || "";
  if (name.length <= 2) return `${name[0] || "*"}*@${domain}`;
  return `${name.slice(0, 2)}***@${domain}`;
}

function setPendingOtp(payload) {
  localStorage.setItem(VA_PILOT_OTP_KEY, JSON.stringify(payload));
}

function getPendingOtp() {
  return JSON.parse(localStorage.getItem(VA_PILOT_OTP_KEY) || "null");
}

function clearPendingOtp() {
  localStorage.removeItem(VA_PILOT_OTP_KEY);
}

function getEmailSendErrorMessage(err) {
  const detail =
    err?.text ||
    err?.message ||
    err?.response?.text ||
    err?.responseText ||
    "unknown error";

  return `Unable to send OTP email (${detail}). Please check EmailJS service/template settings.`;
}

function formatActionType(purpose) {
  const normalized = String(purpose || "").trim().toLowerCase();
  if (normalized === "register") return "Register Account";
  if (normalized === "reset_password") return "Change Password";
  if (normalized === "login") return "Login Verification";
  return String(purpose || "OTP Verification");
}

function buildTemplateParams(email, otp, purpose, name) {
  const actionType = formatActionType(purpose);
  return {
    // Required by your EmailJS template
    email,
    otp_code: otp,
    action_type: actionType,

    // Backward-compatible aliases
    to_email: email,
    to_name: name || "Pilot",
    otp_purpose: purpose,
    expires_in: "5 minutes"
  };
}

async function sendOtpEmail(email, otp, purpose, name) {
  const config = ensureEmailJsReady();
  const templateParams = buildTemplateParams(email, otp, purpose, name);

  try {
    return await emailjs.send(config.serviceId, config.templateId, templateParams);
  } catch (err) {
    throw new Error(getEmailSendErrorMessage(err));
  }
}

async function startOtpFlow(email, purpose, name) {
  const otp = generateOtpCode();
  await sendOtpEmail(email, otp, purpose, name);
  setPendingOtp({
    email,
    purpose,
    name: name || "",
    otp,
    createdAt: Date.now(),
    expiresAt: Date.now() + VA_PILOT_OTP_TTL_MS
  });
}

async function resendOtpFlow() {
  const state = getPendingOtp();
  if (!state) throw new Error("No active OTP request found.");
  return startOtpFlow(state.email, state.purpose, state.name);
}

function verifyOtpCode(code) {
  const state = getPendingOtp();
  if (!state) return { ok: false, reason: "missing" };
  if (Date.now() > state.expiresAt) return { ok: false, reason: "expired", state };
  if (String(code) !== String(state.otp)) return { ok: false, reason: "invalid", state };
  return { ok: true, state };
}

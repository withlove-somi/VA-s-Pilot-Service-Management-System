(() => {
  "use strict";

  const utils = window.VA?.utils;
  const api = window.VA?.api;
  if (!utils || !api) return;

  function getValue(form, selectors) {
    const el = form.querySelector(selectors);
    return el ? el.value : "";
  }

  function clearMessage(form) {
    const msg = form.querySelector("#error-msg") || document.getElementById("error-msg");
    if (msg) msg.classList.add("hidden");
  }

  async function handleLoginSubmit(e) {
    e.preventDefault();
    const form = e.currentTarget;
    clearMessage(form);

    const email = getValue(form, "#email, [name='email'], input[type='email']")
      .trim()
      .toLowerCase();
    const password = getValue(form, "#passcode, [name='password'], [name='passcode'], input[type='password']");

    if (!email || !password) {
      utils.showError("Email and password are required.");
      return;
    }

    const submitBtn = form.querySelector("button[type='submit']");
    if (submitBtn) submitBtn.disabled = true;

    try {
      const payload = await api.login(email, password);
      if (payload?.token) utils.saveAuthToken(payload.token);
      if (payload?.user) utils.saveCurrentUser(payload.user);

      const redirectTo = form.dataset.redirect || "dashboard.html";
      window.location.href = redirectTo;
    } catch (err) {
      utils.showError(err.message || "Login failed. Please try again.");
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  }

  async function handleRegisterSubmit(e) {
    e.preventDefault();
    const form = e.currentTarget;
    clearMessage(form);

    const fullname = getValue(form, "#fullname, [name='fullname'], [name='name']").trim();
    const email = getValue(form, "#email, [name='email'], input[type='email']")
      .trim()
      .toLowerCase();
    const password = getValue(form, "#passcode, [name='password'], [name='passcode'], input[type='password']");
    const confirm = getValue(form, "#confirm-passcode, [name='confirmPassword'], [name='confirm_password']");

    if (!fullname || !email || !password) {
      utils.showError("Full name, email, and password are required.");
      return;
    }

    if (confirm && password !== confirm) {
      utils.showError("Passwords do not match.");
      return;
    }

    const submitBtn = form.querySelector("button[type='submit']");
    if (submitBtn) submitBtn.disabled = true;

    try {
      const payload = await api.register(fullname, email, password);
      if (payload?.token) utils.saveAuthToken(payload.token);
      if (payload?.user) utils.saveCurrentUser(payload.user);

      const redirectTo = form.dataset.redirect || "login.html";
      window.location.href = redirectTo;
    } catch (err) {
      utils.showError(err.message || "Registration failed. Please try again.");
    } finally {
      if (submitBtn) submitBtn.disabled = false;
    }
  }

  const loginForm = document.getElementById("login-form");
  if (loginForm) loginForm.addEventListener("submit", handleLoginSubmit);

  const registerForm = document.getElementById("register-form");
  if (registerForm) registerForm.addEventListener("submit", handleRegisterSubmit);
})();

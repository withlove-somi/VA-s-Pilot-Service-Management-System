package com.vapilot.service.mobile;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static final String PREFS = "va_pilot_prefs";
    private static final String KEY_CURRENT_USER = "va_pilot_current_user";
    private static final String KEY_PENDING_USER = "va_pilot_pending_user";
    private static final String KEY_RESET_EMAIL = "va_pilot_reset_verified_email";
    private static final String KEY_OTP_STATE = "va_pilot_pending_otp";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveCurrentUser(JsonObject user) {
        prefs.edit().putString(KEY_CURRENT_USER, user.toString()).apply();
    }

    public JsonObject getCurrentUser() {
        String raw = prefs.getString(KEY_CURRENT_USER, null);
        if (raw == null) return null;
        return JsonParser.parseString(raw).getAsJsonObject();
    }

    public void clearCurrentUser() {
        prefs.edit().remove(KEY_CURRENT_USER).apply();
    }

    public void savePendingUser(String fullname, String email, String password) {
        Map<String, String> payload = new HashMap<>();
        payload.put("fullname", fullname);
        payload.put("email", email);
        payload.put("password", password);
        prefs.edit().putString(KEY_PENDING_USER, gson.toJson(payload)).apply();
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getPendingUser() {
        String raw = prefs.getString(KEY_PENDING_USER, null);
        if (raw == null) return null;
        return gson.fromJson(raw, Map.class);
    }

    public void clearPendingUser() {
        prefs.edit().remove(KEY_PENDING_USER).apply();
    }

    public void saveResetVerifiedEmail(String email) {
        prefs.edit().putString(KEY_RESET_EMAIL, email).apply();
    }

    public String getResetVerifiedEmail() {
        return prefs.getString(KEY_RESET_EMAIL, null);
    }

    public void clearResetVerifiedEmail() {
        prefs.edit().remove(KEY_RESET_EMAIL).apply();
    }

    public void saveOtpState(OtpState state) {
        prefs.edit().putString(KEY_OTP_STATE, gson.toJson(state)).apply();
    }

    public OtpState getOtpState() {
        String raw = prefs.getString(KEY_OTP_STATE, null);
        if (raw == null) return null;
        return gson.fromJson(raw, OtpState.class);
    }

    public void clearOtpState() {
        prefs.edit().remove(KEY_OTP_STATE).apply();
    }
}

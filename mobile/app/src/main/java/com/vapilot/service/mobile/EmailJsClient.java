package com.vapilot.service.mobile;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONObject;

public final class EmailJsClient {
    private static final String EMAILJS_ENDPOINT = "https://api.emailjs.com/api/v1.0/email/send";

    public static final String PUBLIC_KEY = "SgkQyK2k1yc7lGO_R";
    public static final String SERVICE_ID = "service_8j4tgje";
    public static final String TEMPLATE_ID = "template_b4qhvvg";

    private static final OkHttpClient httpClient = new OkHttpClient();

    private EmailJsClient() {
    }

    public static void sendOtpEmail(String email, String otp, String purpose, String name) throws Exception {
        JSONObject templateParams = new JSONObject();
        templateParams.put("email", email);
        templateParams.put("otp_code", otp);
        templateParams.put("action_type", formatActionType(purpose));
        templateParams.put("to_email", email);
        templateParams.put("to_name", name == null || name.isEmpty() ? "Pilot" : name);
        templateParams.put("otp_purpose", purpose);
        templateParams.put("expires_in", "5 minutes");

        JSONObject payload = new JSONObject();
        payload.put("service_id", SERVICE_ID);
        payload.put("template_id", TEMPLATE_ID);
        payload.put("user_id", PUBLIC_KEY);
        payload.put("template_params", templateParams);

        Request request = new Request.Builder()
                .url(EMAILJS_ENDPOINT)
                .post(RequestBody.create(payload.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String detail = response.body() != null ? response.body().string() : "unknown error";
                throw new Exception("Unable to send OTP email (" + detail + ").");
            }
        }
    }

    private static String formatActionType(String purpose) {
        if ("register".equalsIgnoreCase(purpose)) return "Register Account";
        if ("reset_password".equalsIgnoreCase(purpose)) return "Change Password";
        if ("login".equalsIgnoreCase(purpose)) return "Login Verification";
        return "OTP Verification";
    }
}

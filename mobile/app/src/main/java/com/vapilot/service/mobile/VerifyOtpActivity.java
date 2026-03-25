package com.vapilot.service.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class VerifyOtpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        TextView description = findViewById(R.id.otp_description);
        TextInputEditText codeInput = findViewById(R.id.otp_code);
        Button verifyButton = findViewById(R.id.otp_verify_button);
        Button resendButton = findViewById(R.id.otp_resend_button);
        TextView errorText = findViewById(R.id.otp_error);

        SessionManager session = new SessionManager(this);
        OtpState state = session.getOtpState();
        description.setText(state == null
                ? "No active OTP request found. Please start again."
                : "Enter the 6-digit code sent to " + state.email + "."
        );

        verifyButton.setOnClickListener(v -> {
            String code = safeString(codeInput.getText());
            if (code.length() != 6) {
                showError(errorText, "Enter the full 6-digit OTP.");
                return;
            }

            OtpVerifyResult result = OtpManager.verifyOtp(session, code);
            if (!result.ok) {
                String message;
                if ("expired".equals(result.reason)) message = "OTP expired. Please resend a new code.";
                else if ("missing".equals(result.reason)) message = "No active OTP request found.";
                else message = "Invalid OTP. Please try again.";
                showError(errorText, message);
                return;
            }

            String purpose = result.state != null ? result.state.purpose : "";
            String email = result.state != null ? result.state.email : "";

            AppExecutors.io().execute(() -> {
                try {
                    if ("reset_password".equals(purpose)) {
                        session.saveResetVerifiedEmail(email);
                        session.clearOtpState();
                        AppExecutors.runOnMain(() -> {
                            startActivity(new Intent(this, ResetPasswordActivity.class));
                            finish();
                        });
                        return;
                    }

                    if ("register".equals(purpose)) {
                        Map<String, String> pending = session.getPendingUser();
                        if (pending == null || !email.equalsIgnoreCase(pending.get("email"))) {
                            AppExecutors.runOnMain(() ->
                                    showError(errorText, "Registration session mismatch. Please register again."));
                            return;
                        }

                        Map<String, String> body = new HashMap<>();
                        body.put("fullname", pending.get("fullname"));
                        body.put("email", pending.get("email"));
                        body.put("password", pending.get("password"));
                        ApiHelper.execute(ApiClient.getService().register(body));

                        session.clearPendingUser();
                        session.clearOtpState();
                        AppExecutors.runOnMain(() -> {
                            startActivity(new Intent(this, LoginActivity.class));
                            finish();
                        });
                        return;
                    }

                    if ("login".equals(purpose)) {
                        JsonObject payload = ApiHelper.execute(ApiClient.getService().getUser(email));
                        JsonObject user = payload.has("user") && payload.get("user").isJsonObject()
                                ? payload.getAsJsonObject("user")
                                : payload;
                        session.saveCurrentUser(user);
                        session.clearOtpState();
                        AppExecutors.runOnMain(() -> {
                            startActivity(new Intent(this, DashboardActivity.class));
                            finish();
                        });
                        return;
                    }

                    AppExecutors.runOnMain(() ->
                            showError(errorText, "Unknown OTP flow. Please start again."));
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() ->
                            showError(errorText, ex.getMessage() == null ? "Unable to continue." : ex.getMessage()));
                }
            });
        });

        resendButton.setOnClickListener(v -> AppExecutors.io().execute(() -> {
            try {
                OtpManager.resendOtpFlow(session);
                AppExecutors.runOnMain(() -> errorText.setVisibility(View.GONE));
            } catch (Exception ex) {
                AppExecutors.runOnMain(() ->
                        showError(errorText, ex.getMessage() == null ? "Unable to resend OTP right now." : ex.getMessage()));
            }
        }));
    }

    private String safeString(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void showError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }
}

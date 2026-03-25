package com.vapilot.service.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        TextInputEditText fullnameInput = findViewById(R.id.register_fullname);
        TextInputEditText emailInput = findViewById(R.id.register_email);
        TextInputEditText passwordInput = findViewById(R.id.register_password);
        TextInputEditText confirmInput = findViewById(R.id.register_confirm);
        Button submitButton = findViewById(R.id.register_button);
        TextView errorText = findViewById(R.id.register_error);

        submitButton.setOnClickListener(v -> {
            String fullname = safeString(fullnameInput.getText());
            String email = safeLower(emailInput.getText());
            String password = safeString(passwordInput.getText());
            String confirm = safeString(confirmInput.getText());

            if (fullname.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showError(errorText, "All fields are required.");
                return;
            }
            if (!password.equals(confirm)) {
                showError(errorText, "Passwords do not match.");
                return;
            }

            errorText.setVisibility(View.GONE);
            submitButton.setEnabled(false);
            submitButton.setText("SENDING OTP...");

            AppExecutors.io().execute(() -> {
                try {
                    // Check if user exists
                    try {
                        Call<com.google.gson.JsonObject> call = ApiClient.getService().getUser(email);
                        ApiHelper.execute(call);
                        AppExecutors.runOnMain(() -> showError(errorText, "Email already registered."));
                        return;
                    } catch (Exception ignored) {
                        // expected if user not found
                    }

                    SessionManager session = new SessionManager(this);
                    session.savePendingUser(fullname, email, password);
                    OtpManager.startOtpFlow(session, email, "register", fullname);

                    AppExecutors.runOnMain(() -> {
                        startActivity(new Intent(this, VerifyOtpActivity.class));
                        finish();
                    });
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() -> showError(errorText, ex.getMessage() == null
                            ? "Unable to send OTP right now."
                            : ex.getMessage()));
                } finally {
                    AppExecutors.runOnMain(() -> {
                        submitButton.setEnabled(true);
                        submitButton.setText("Send OTP");
                    });
                }
            });
        });
    }

    private String safeLower(CharSequence value) {
        return value == null ? "" : value.toString().trim().toLowerCase();
    }

    private String safeString(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void showError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }
}

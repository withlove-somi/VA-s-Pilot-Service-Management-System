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

public class ResetPasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        TextInputEditText passwordInput = findViewById(R.id.reset_password);
        TextInputEditText confirmInput = findViewById(R.id.reset_confirm);
        Button button = findViewById(R.id.reset_button);
        TextView errorText = findViewById(R.id.reset_error);

        SessionManager session = new SessionManager(this);
        String email = session.getResetVerifiedEmail();
        if (email == null || email.isEmpty()) {
            showError(errorText, "No verified email found. Please start again.");
        }

        button.setOnClickListener(v -> {
            String password = safeString(passwordInput.getText());
            String confirm = safeString(confirmInput.getText());
            if (password.isEmpty() || confirm.isEmpty()) {
                showError(errorText, "All fields are required.");
                return;
            }
            if (!password.equals(confirm)) {
                showError(errorText, "Passwords do not match.");
                return;
            }
            if (email == null || email.isEmpty()) {
                showError(errorText, "No verified email found. Please start again.");
                return;
            }

            errorText.setVisibility(View.GONE);
            AppExecutors.io().execute(() -> {
                try {
                    Map<String, String> body = new HashMap<>();
                    body.put("email", email);
                    body.put("newPassword", password);
                    ApiHelper.execute(ApiClient.getService().resetPassword(body));
                    session.clearResetVerifiedEmail();
                    AppExecutors.runOnMain(() -> {
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() ->
                            showError(errorText, ex.getMessage() == null ? "Unable to update password." : ex.getMessage()));
                }
            });
        });
    }

    private String safeString(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void showError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }
}

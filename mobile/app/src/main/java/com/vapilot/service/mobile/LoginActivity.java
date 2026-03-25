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

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextInputEditText emailInput = findViewById(R.id.login_email);
        TextInputEditText passwordInput = findViewById(R.id.login_password);
        Button loginButton = findViewById(R.id.login_button);
        TextView errorText = findViewById(R.id.login_error);
        TextView forgotLink = findViewById(R.id.login_forgot);
        TextView registerLink = findViewById(R.id.login_register);

        forgotLink.setOnClickListener(v ->
                startActivity(new Intent(this, ForgotPasswordActivity.class))
        );

        registerLink.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );

        loginButton.setOnClickListener(v -> {
            String email = safeLower(emailInput.getText());
            String password = safeString(passwordInput.getText());
            if (email.isEmpty() || password.isEmpty()) {
                showError(errorText, "Email and password are required.");
                return;
            }

            errorText.setVisibility(View.GONE);
            AppExecutors.io().execute(() -> {
                try {
                    Map<String, String> body = new HashMap<>();
                    body.put("email", email);
                    body.put("password", password);

                    JsonObject payload = ApiHelper.execute(ApiClient.getService().login(body));
                    JsonObject user = payload.has("user") && payload.get("user").isJsonObject()
                            ? payload.getAsJsonObject("user")
                            : payload;

                    new SessionManager(this).saveCurrentUser(user);
                    AppExecutors.runOnMain(() -> {
                        startActivity(new Intent(this, DashboardActivity.class));
                        finish();
                    });
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() -> showError(errorText, "Invalid email or password."));
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

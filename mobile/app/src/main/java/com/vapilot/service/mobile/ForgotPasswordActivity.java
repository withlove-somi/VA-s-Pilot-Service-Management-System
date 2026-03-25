package com.vapilot.service.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

public class ForgotPasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        TextInputEditText emailInput = findViewById(R.id.forgot_email);
        Button button = findViewById(R.id.forgot_button);
        TextView errorText = findViewById(R.id.forgot_error);

        button.setOnClickListener(v -> {
            String email = safeLower(emailInput.getText());
            if (email.isEmpty()) {
                showError(errorText, "Email is required.");
                return;
            }

            errorText.setVisibility(View.GONE);
            AppExecutors.io().execute(() -> {
                try {
                    JsonObject payload = ApiHelper.execute(ApiClient.getService().getUser(email));
                    JsonObject user = payload.has("user") && payload.get("user").isJsonObject()
                            ? payload.getAsJsonObject("user")
                            : payload;
                    String name = user.has("fullname") ? user.get("fullname").getAsString()
                            : (user.has("name") ? user.get("name").getAsString() : "Pilot");

                    SessionManager session = new SessionManager(this);
                    OtpManager.startOtpFlow(session, email, "reset_password", name);
                    AppExecutors.runOnMain(() -> {
                        startActivity(new Intent(this, VerifyOtpActivity.class));
                        finish();
                    });
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() ->
                            showError(errorText, ex.getMessage() == null ? "Email not registered." : ex.getMessage()));
                }
            });
        });
    }

    private String safeLower(CharSequence value) {
        return value == null ? "" : value.toString().trim().toLowerCase();
    }

    private void showError(TextView view, String message) {
        view.setText(message);
        view.setVisibility(View.VISIBLE);
    }
}

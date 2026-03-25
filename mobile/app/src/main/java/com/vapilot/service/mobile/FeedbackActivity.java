package com.vapilot.service.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends BaseDrawerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawer(R.layout.activity_feedback, "Feedback");

        SessionManager session = new SessionManager(this);
        JsonObject user = session.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        TextInputEditText nameInput = findViewById(R.id.feedback_name);
        TextInputEditText emailInput = findViewById(R.id.feedback_email);
        TextInputEditText ratingInput = findViewById(R.id.feedback_rating);
        TextInputEditText messageInput = findViewById(R.id.feedback_message);
        Button submit = findViewById(R.id.feedback_submit);
        TextView errorText = findViewById(R.id.feedback_error);

        String name = user.has("fullname") ? user.get("fullname").getAsString()
                : (user.has("name") ? user.get("name").getAsString() : "");
        String email = user.has("email") ? user.get("email").getAsString() : "";
        nameInput.setText(name);
        emailInput.setText(email);

        submit.setOnClickListener(v -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", user.has("id") ? user.get("id").getAsString() : null);
            payload.put("userEmail", email);
            payload.put("name", safeString(nameInput.getText()));
            payload.put("email", safeString(emailInput.getText()));
            payload.put("rating", safeString(ratingInput.getText()));
            payload.put("message", safeString(messageInput.getText()));

            if (safeString(messageInput.getText()).isEmpty()) {
                showError(errorText, "Please enter your feedback.");
                return;
            }

            AppExecutors.io().execute(() -> {
                try {
                    ApiHelper.execute(ApiClient.getService().sendFeedback(payload));
                    AppExecutors.runOnMain(() -> {
                        errorText.setText("Thanks for your feedback!");
                        errorText.setVisibility(View.VISIBLE);
                        messageInput.setText("");
                    });
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() ->
                            showError(errorText, ex.getMessage() == null ? "Unable to submit feedback." : ex.getMessage()));
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

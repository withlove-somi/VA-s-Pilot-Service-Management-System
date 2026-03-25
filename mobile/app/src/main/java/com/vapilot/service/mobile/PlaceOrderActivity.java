package com.vapilot.service.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class PlaceOrderActivity extends BaseDrawerActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawer(R.layout.activity_place_order, "Place Order");

        SessionManager session = new SessionManager(this);
        JsonObject user = session.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        TextInputEditText fullnameInput = findViewById(R.id.order_fullname);
        TextInputEditText emailInput = findViewById(R.id.order_email);
        TextInputEditText ignInput = findViewById(R.id.order_ign);
        TextInputEditText accountIdInput = findViewById(R.id.order_account_id);
        TextInputEditText serverIdInput = findViewById(R.id.order_server_id);
        TextInputEditText pilotMethodInput = findViewById(R.id.order_pilot_method);
        TextInputEditText pilotEmailInput = findViewById(R.id.order_pilot_email);
        TextInputEditText pilotPasswordInput = findViewById(R.id.order_pilot_password);
        TextInputEditText pilotNumberInput = findViewById(R.id.order_pilot_number);
        TextInputEditText gameInput = findViewById(R.id.order_game);
        TextInputEditText currentRankInput = findViewById(R.id.order_current_rank);
        TextInputEditText targetRankInput = findViewById(R.id.order_target_rank);
        TextInputEditText starsInput = findViewById(R.id.order_stars_needed);
        TextInputEditText notesInput = findViewById(R.id.order_notes);
        TextInputEditText paymentModeInput = findViewById(R.id.order_payment_mode);
        TextInputEditText totalPriceInput = findViewById(R.id.order_total_price);
        Button submitButton = findViewById(R.id.order_submit);
        TextView errorText = findViewById(R.id.order_error);

        String name = user.has("fullname") ? user.get("fullname").getAsString()
                : (user.has("name") ? user.get("name").getAsString() : "");
        String email = user.has("email") ? user.get("email").getAsString() : "";
        fullnameInput.setText(name);
        emailInput.setText(email);

        submitButton.setOnClickListener(v -> {
            int starsNeeded = 0;
            try {
                starsNeeded = Integer.parseInt(safeString(starsInput.getText()));
            } catch (Exception ignored) {
            }

            Map<String, Object> order = new HashMap<>();
            order.put("id", "ORD-" + (100000 + new Random().nextInt(900000)));
            order.put("date", new SimpleDateFormat("M/d/yyyy", Locale.getDefault()).format(new Date()));
            order.put("customerName", safeString(fullnameInput.getText()));
            order.put("customerEmail", email);
            order.put("customerVerified", user.has("verified") && user.get("verified").getAsBoolean());
            order.put("ign", safeString(ignInput.getText()));
            order.put("accountId", safeString(accountIdInput.getText()));
            order.put("serverId", safeString(serverIdInput.getText()));
            order.put("pilotLoginMethod", safeString(pilotMethodInput.getText()));
            order.put("pilotLoginEmail", safeString(pilotEmailInput.getText()));
            order.put("pilotLoginPassword", safeString(pilotPasswordInput.getText()));
            order.put("pilotLoginNumber", safeString(pilotNumberInput.getText()));
            order.put("game", safeString(gameInput.getText()));
            order.put("currentRank", safeString(currentRankInput.getText()));
            order.put("targetRank", safeString(targetRankInput.getText()));
            order.put("starsNeeded", starsNeeded);
            String notes = safeString(notesInput.getText());
            order.put("notes", notes.isEmpty() ? "None" : notes);
            order.put("paymentMode", safeString(paymentModeInput.getText()));
            order.put("totalPrice", safeString(totalPriceInput.getText()));
            order.put("status", "Order Request");

            errorText.setVisibility(View.GONE);
            AppExecutors.io().execute(() -> {
                try {
                    ApiHelper.execute(ApiClient.getService().createOrder(order));
                    AppExecutors.runOnMain(() -> {
                        startActivity(new Intent(this, OrdersActivity.class));
                        finish();
                    });
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() -> {
                        errorText.setText(ex.getMessage() == null ? "Unable to place order right now." : ex.getMessage());
                        errorText.setVisibility(View.VISIBLE);
                    });
                }
            });
        });
    }

    private String safeString(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }
}

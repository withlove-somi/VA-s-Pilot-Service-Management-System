package com.vapilot.service.mobile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentActivity extends BaseDrawerActivity {
    private SimpleListAdapter adapter;
    private String receiptImage;

    private final ActivityResultLauncher<String> pickReceipt =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    try {
                        receiptImage = ImageUtils.uriToBase64(this, uri);
                    } catch (Exception ignored) {
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawer(R.layout.activity_payment, "Payment");

        SessionManager session = new SessionManager(this);
        JsonObject user = session.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        RecyclerView recycler = findViewById(R.id.payment_orders);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleListAdapter();
        recycler.setAdapter(adapter);

        Button refreshButton = findViewById(R.id.payment_refresh);
        refreshButton.setOnClickListener(v -> loadOrders());

        TextInputEditText orderIdInput = findViewById(R.id.payment_order_id);
        TextInputEditText amountInput = findViewById(R.id.payment_amount);
        TextInputEditText refInput = findViewById(R.id.payment_reference);
        Button pickButton = findViewById(R.id.payment_pick);
        Button submitButton = findViewById(R.id.payment_submit);
        TextView errorText = findViewById(R.id.payment_error);

        pickButton.setOnClickListener(v -> pickReceipt.launch("image/*"));

        submitButton.setOnClickListener(v -> {
            String orderId = safeString(orderIdInput.getText());
            String ref = safeString(refInput.getText());
            double amount = 0.0;
            try {
                amount = Double.parseDouble(safeString(amountInput.getText()));
            } catch (Exception ignored) {
            }

            if (orderId.isEmpty() || ref.isEmpty() || receiptImage == null) {
                showError(errorText, "Order ID, reference, and receipt image are required.");
                return;
            }

            double finalAmount = amount;
            AppExecutors.io().execute(() -> {
                try {
                    Map<String, Object> body = new HashMap<>();
                    body.put("orderId", orderId);
                    body.put("amount", finalAmount);
                    body.put("paymentReference", ref);
                    body.put("receiptImage", receiptImage);
                    ApiHelper.execute(ApiClient.getService().createTransaction(body));
                    AppExecutors.runOnMain(() -> errorText.setVisibility(View.GONE));
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() ->
                            showError(errorText, ex.getMessage() == null ? "Unable to submit payment." : ex.getMessage()));
                }
            });
        });

        loadOrders();
    }

    private void loadOrders() {
        AppExecutors.io().execute(() -> {
            try {
                JsonObject payload = ApiHelper.execute(ApiClient.getService().getAllOrders());
                JsonArray orders = payload.has("orders") ? payload.getAsJsonArray("orders") : new JsonArray();
                List<String> list = new ArrayList<>();
                for (JsonElement element : orders) {
                    JsonObject obj = element.getAsJsonObject();
                    String id = obj.has("id") ? obj.get("id").getAsString() : "Order";
                    String status = obj.has("status") ? obj.get("status").getAsString() : "Unknown";
                    String total = obj.has("totalPrice") ? obj.get("totalPrice").getAsString() : "0";
                    list.add(id + " • " + status + " • ₱" + total);
                }
                AppExecutors.runOnMain(() -> adapter.submitList(list));
            } catch (Exception ex) {
                List<String> list = new ArrayList<>();
                list.add("Unable to load orders: " + ex.getMessage());
                AppExecutors.runOnMain(() -> adapter.submitList(list));
            }
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

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

public class OrdersActivity extends BaseDrawerActivity {
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
        setupDrawer(R.layout.activity_orders, "Orders");

        SessionManager session = new SessionManager(this);
        JsonObject user = session.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        String email = user.has("email") ? user.get("email").getAsString() : "";

        RecyclerView recycler = findViewById(R.id.orders_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleListAdapter();
        recycler.setAdapter(adapter);

        Button refreshBtn = findViewById(R.id.orders_refresh);
        refreshBtn.setOnClickListener(v -> loadOrders(email));

        TextInputEditText statusId = findViewById(R.id.orders_status_id);
        TextInputEditText statusValue = findViewById(R.id.orders_status_value);
        Button statusButton = findViewById(R.id.orders_status_button);

        TextInputEditText payId = findViewById(R.id.orders_payment_id);
        TextInputEditText payRef = findViewById(R.id.orders_payment_ref);
        Button pickButton = findViewById(R.id.orders_payment_pick);
        Button payButton = findViewById(R.id.orders_payment_button);
        TextView errorText = findViewById(R.id.orders_error);

        pickButton.setOnClickListener(v -> pickReceipt.launch("image/*"));

        statusButton.setOnClickListener(v -> {
            String orderId = safeString(statusId.getText());
            String status = safeString(statusValue.getText());
            if (orderId.isEmpty() || status.isEmpty()) {
                showError(errorText, "Order ID and status are required.");
                return;
            }

            AppExecutors.io().execute(() -> {
                try {
                    Map<String, String> body = new HashMap<>();
                    body.put("status", status);
                    ApiHelper.execute(ApiClient.getService().updateOrderStatus(orderId, body));
                    loadOrders(email);
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() ->
                            showError(errorText, ex.getMessage() == null ? "Unable to update order status." : ex.getMessage()));
                }
            });
        });

        payButton.setOnClickListener(v -> {
            String orderId = safeString(payId.getText());
            String ref = safeString(payRef.getText());
            if (orderId.isEmpty() || ref.isEmpty() || receiptImage == null) {
                showError(errorText, "Order ID, reference, and receipt image are required.");
                return;
            }

            AppExecutors.io().execute(() -> {
                try {
                    Map<String, String> body = new HashMap<>();
                    body.put("paymentReference", ref);
                    body.put("receiptImage", receiptImage);
                    ApiHelper.execute(ApiClient.getService().updateOrderPayment(orderId, body));
                    loadOrders(email);
                } catch (Exception ex) {
                    AppExecutors.runOnMain(() ->
                            showError(errorText, ex.getMessage() == null ? "Unable to submit payment." : ex.getMessage()));
                }
            });
        });

        loadOrders(email);
    }

    private void loadOrders(String email) {
        AppExecutors.io().execute(() -> {
            try {
                JsonObject payload = ApiHelper.execute(ApiClient.getService().getOrders(email));
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

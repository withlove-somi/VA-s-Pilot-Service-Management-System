package com.vapilot.service.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends BaseDrawerActivity {
    private SimpleListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawer(R.layout.activity_dashboard, "Dashboard");

        SessionManager session = new SessionManager(this);
        JsonObject user = session.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String name = user.has("fullname") ? user.get("fullname").getAsString()
                : (user.has("name") ? user.get("name").getAsString() : "Customer");
        String email = user.has("email") ? user.get("email").getAsString() : "";
        boolean verified = user.has("verified") && user.get("verified").getAsBoolean();

        ((TextView) findViewById(R.id.dashboard_user)).setText(name);
        ((TextView) findViewById(R.id.dashboard_email)).setText(email);
        ((TextView) findViewById(R.id.dashboard_verified)).setText("Verified: " + (verified ? "Yes" : "No"));

        RecyclerView recycler = findViewById(R.id.dashboard_orders);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleListAdapter();
        recycler.setAdapter(adapter);

        Button refreshBtn = findViewById(R.id.dashboard_refresh);
        refreshBtn.setOnClickListener(v -> loadOrders(email));

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
}

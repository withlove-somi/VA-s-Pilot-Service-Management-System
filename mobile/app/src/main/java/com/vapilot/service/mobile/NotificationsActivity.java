package com.vapilot.service.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends BaseDrawerActivity {
    private SimpleListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupDrawer(R.layout.activity_notifications, "Notifications");

        SessionManager session = new SessionManager(this);
        JsonObject user = session.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        String email = user.has("email") ? user.get("email").getAsString() : "";

        RecyclerView recycler = findViewById(R.id.notifications_list);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SimpleListAdapter();
        recycler.setAdapter(adapter);

        Button refreshBtn = findViewById(R.id.notifications_refresh);
        refreshBtn.setOnClickListener(v -> loadNotifications(email));

        loadNotifications(email);
    }

    private void loadNotifications(String email) {
        AppExecutors.io().execute(() -> {
            try {
                JsonObject payload = ApiHelper.execute(ApiClient.getService().getNotifications(email));
                JsonArray notifications = payload.has("notifications")
                        ? payload.getAsJsonArray("notifications")
                        : new JsonArray();
                List<String> list = new ArrayList<>();
                for (JsonElement element : notifications) {
                    JsonObject obj = element.getAsJsonObject();
                    String title = obj.has("title") ? obj.get("title").getAsString() : "Notification";
                    String message = obj.has("message") ? obj.get("message").getAsString() : "";
                    list.add(title + " • " + message);
                }
                AppExecutors.runOnMain(() -> adapter.submitList(list));
            } catch (Exception ex) {
                List<String> list = new ArrayList<>();
                list.add("Unable to load notifications: " + ex.getMessage());
                AppExecutors.runOnMain(() -> adapter.submitList(list));
            }
        });
    }
}

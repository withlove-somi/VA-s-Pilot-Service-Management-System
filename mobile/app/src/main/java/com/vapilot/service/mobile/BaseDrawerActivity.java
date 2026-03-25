package com.vapilot.service.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

public abstract class BaseDrawerActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    protected DrawerLayout drawerLayout;
    protected FrameLayout contentFrame;
    protected NavigationView navigationView;
    protected Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the updated XML
        setContentView(R.layout.activity_drawer);

        drawerLayout = findViewById(R.id.drawer_layout);
        contentFrame = findViewById(R.id.content_frame);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        // This adds the standard Android smooth expand/collapse hamburger icon behavior
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Hook up the new Chat Modal Replacement box inside the drawer
        View chatCard = findViewById(R.id.sidebar_chat_card);
        if (chatCard != null) {
            chatCard.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                // Currently shows a toast, but you can change this to start a ChatActivity later
                Toast.makeText(this, "Opening Live Chat...", Toast.LENGTH_SHORT).show();
            });
        }
    }

    // Your existing routing logic. DO NOT change the contents inside this.
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        handleNav(item.getItemId());
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    // Abstract method so child activities handle their own intents safely
    protected abstract void handleNav(int itemId);

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
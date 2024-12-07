package com.example.mobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SpeedTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speed_test);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new SpeedTestFragment())
                .commit();
        }

        // Setup bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_map) {
                startActivity(new Intent(this, MapActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_speed) {
                // Already on speed test activity
                return true;
            } else if (itemId == R.id.navigation_stats) {
                startActivity(new Intent(this, StatisticsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return false;
        });

        // Set the speed test item as selected
        bottomNav.setSelectedItemId(R.id.navigation_speed);
    }
}

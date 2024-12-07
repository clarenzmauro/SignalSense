package com.example.mobilecomputing;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private TextView userNameText;
    private TextView emailText;
    private TextView accountCreatedText;
    private TextView lastLoginText;
    private Button signOutButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        initializeViews();
        setupBottomNavigation();
        loadUserProfile();
    }

    private void initializeViews() {
        userNameText = findViewById(R.id.userNameText);
        emailText = findViewById(R.id.emailText);
        accountCreatedText = findViewById(R.id.accountCreatedText);
        lastLoginText = findViewById(R.id.lastLoginText);
        signOutButton = findViewById(R.id.signOutButton);

        signOutButton.setOnClickListener(v -> signOut());
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_map) {
                startActivity(new Intent(this, MapActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_speed) {
                startActivity(new Intent(this, SpeedTestActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_stats) {
                startActivity(new Intent(this, StatisticsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.navigation_profile) {
                // Already on profile activity
                return true;
            }
            return false;
        });

        bottomNav.setSelectedItemId(R.id.navigation_profile);
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Display user information
            String displayName = user.getDisplayName();
            String email = user.getEmail();
            long creationTimestamp = user.getMetadata().getCreationTimestamp();
            long lastSignInTimestamp = user.getMetadata().getLastSignInTimestamp();

            userNameText.setText(displayName != null ? displayName : "User");
            emailText.setText(email);
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            accountCreatedText.setText("Account Created: " + sdf.format(new Date(creationTimestamp)));
            lastLoginText.setText("Last Login: " + sdf.format(new Date(lastSignInTimestamp)));
        } else {
            // User is not logged in, redirect to login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void signOut() {
        mAuth.signOut();
        // Clear any user preferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();

        // Redirect to login
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verify user is still logged in
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}

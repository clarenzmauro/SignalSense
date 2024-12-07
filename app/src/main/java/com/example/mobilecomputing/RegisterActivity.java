package com.example.mobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // UI Elements
    EditText username, password, email;
    Button registerButton;

    // Firebase Instances
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI Elements
        username = findViewById(R.id.unm);
        password = findViewById(R.id.pwd);
        email = findViewById(R.id.eid);
        registerButton = findViewById(R.id.regi);

        // Set Register Button Click Listener
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userNameInput = username.getText().toString().trim();
                String passwordInput = password.getText().toString().trim();
                String emailInput = email.getText().toString().trim();

                // Validate Input
                if (TextUtils.isEmpty(userNameInput)) {
                    username.setError("Username is required");
                    username.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(passwordInput)) {
                    password.setError("Password is required");
                    password.requestFocus();
                    return;
                }

                if (passwordInput.length() < 6) {
                    password.setError("Password should be at least 6 characters");
                    password.requestFocus();
                    return;
                }

                if (TextUtils.isEmpty(emailInput)) {
                    email.setError("Email is required");
                    email.requestFocus();
                    return;
                }

                // Register User
                registerUser(emailInput, passwordInput, userNameInput);
            }
        });
    }

    private void registerUser(String email, String password, String username) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // User Registration Successful
                            saveUserDetails(username, email);
                        } else {
                            // Registration Failed
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void saveUserDetails(String username, String email) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();

            // Save User Details in Firestore
            Map<String, Object> userData = new HashMap<>();
            userData.put("username", username);
            userData.put("email", email);

            db.collection("users").document(userId)
                    .set(userData)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Data Saved Successfully
                            Toast.makeText(RegisterActivity.this, "User registered successfully", Toast.LENGTH_SHORT).show();

                            // Navigate to Login Page
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Error Saving Data
                            Toast.makeText(RegisterActivity.this, "Error saving user details: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}

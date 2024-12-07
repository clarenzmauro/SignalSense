package com.example.mobilecomputing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100; // Request code for Google Sign-In
    private static final String TAG = "MainActivity";

    private EditText emailpno, pwd;
    private Button submit;
    private TextView sgnup, forgotpwd;
    private com.google.android.gms.common.SignInButton googleSignInButton;
    private ImageView eyeIcon;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailpno = findViewById(R.id.logineml);
        pwd = findViewById(R.id.loginpwd);
        submit = findViewById(R.id.log);
        sgnup = findViewById(R.id.signupTextView);
        forgotpwd = findViewById(R.id.resetpassword);
        googleSignInButton = findViewById(R.id.sgnin);
        eyeIcon = findViewById(R.id.eye_icon);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // From google-services.json
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Set up password visibility toggle
        eyeIcon.setOnClickListener(v -> {
            if (pwd.getTransformationMethod() instanceof PasswordTransformationMethod) {
                // Show password
                pwd.setTransformationMethod(null);
                eyeIcon.setImageResource(R.drawable.ic_eye_on);
            } else {
                // Hide password
                pwd.setTransformationMethod(new PasswordTransformationMethod());
                eyeIcon.setImageResource(R.drawable.ic_eye_off);
            }
            // Move cursor to the end of text
            pwd.setSelection(pwd.getText().length());
        });

        // Set listeners
        submit.setOnClickListener(view -> loginWithEmailAndPassword());
        googleSignInButton.setOnClickListener(view -> signInWithGoogle());
        sgnup.setOnClickListener(view -> navigateToSignup());
        forgotpwd.setOnClickListener(view -> navigateToForgotPassword());
    }

    private void loginWithEmailAndPassword() {
        String email = emailpno.getText().toString().trim();
        String password = pwd.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailpno.setError("Email is required");
            emailpno.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            pwd.setError("Password is required");
            pwd.requestFocus();
            return;
        }

        if (password.length() < 6) {
            pwd.setError("Password should be at least 6 characters");
            pwd.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        navigateToHome(user);
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(Exception.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                }
            } catch (Exception e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(this, "Google sign-in successful", Toast.LENGTH_SHORT).show();
                        navigateToHome(user);
                    } else {
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToSignup() {
        Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
        startActivity(intent);
        finish(); // Optional: Call finish() if you don't want the user to return to the login screen by pressing back
    }

    private void navigateToForgotPassword() {
        String email = emailpno.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            emailpno.setError("Enter your email to reset password");
            emailpno.requestFocus();
        } else {
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void navigateToHome(FirebaseUser user) {
        // Navigate to the main app screen
        Toast.makeText(this, "Welcome, " + user.getEmail(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MapActivity.class);
        startActivity(intent);
        finish();
    }
}

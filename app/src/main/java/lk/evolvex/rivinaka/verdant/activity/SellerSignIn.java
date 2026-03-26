package lk.evolvex.rivinaka.verdant.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.evolvex.rivinaka.verdant.R;

public class SellerSignIn extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private Button btnSignIn;
    private TextView tvSignUp, tvForgotPassword;
    private CheckBox cbRememberMe;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "SellerLoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "remember_me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seller_sign_in);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvSignUp = findViewById(R.id.tvSignUp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        loadSavedCredentials();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnSignIn.setOnClickListener(v -> loginSeller());

        tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SellerSignIn.this, SellerSignUp.class);
            startActivity(intent);
        });

        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(SellerSignIn.this, ForgotPasswordActivity.class);
            intent.putExtra("isSeller", true);
            startActivity(intent);
        });
    }

    private void loadSavedCredentials() {
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        cbRememberMe.setChecked(rememberMe);
        if (rememberMe) {
            String email = sharedPreferences.getString(KEY_EMAIL, "");
            String password = sharedPreferences.getString(KEY_PASSWORD, "");
            etEmail.setText(email);
            etPassword.setText(password);
        }
    }

    private void saveCredentials(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (cbRememberMe.isChecked()) {
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
            editor.putBoolean(KEY_REMEMBER_ME, true);
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private void loginSeller() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignIn.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    saveCredentials(email, password);
                    checkIfSeller(authResult.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    btnSignIn.setEnabled(true);
                    Toast.makeText(this, "Login Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkIfSeller(String uid) {
        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        
                        // Using case-insensitive comparison for "Deactive" and "Deactivated"
                        if (status != null && (status.equalsIgnoreCase("Deactive") || status.equalsIgnoreCase("Deactivated"))) {
                            mAuth.signOut();
                            btnSignIn.setEnabled(true);
                            Toast.makeText(this, "Your account is deactivated. Please contact support.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String role = documentSnapshot.getString("role");
                        if ("seller".equals(role)) {
                            startActivity(new Intent(SellerSignIn.this, SellerMainHome.class));
                            finish();
                        } else {
                            mAuth.signOut();
                            btnSignIn.setEnabled(true);
                            Toast.makeText(this, "Not a seller account", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        mAuth.signOut();
                        btnSignIn.setEnabled(true);
                        Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    btnSignIn.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

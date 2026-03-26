package lk.evolvex.rivinaka.verdant.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.evolvex.rivinaka.verdant.databinding.ActivitySignInBinding;

public class SignIn extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "CustomerLoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "remember_me";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        setupTextWatchers();
        loadSavedCredentials();

        binding.btnSignIn.setOnClickListener(v -> {
            String email = binding.tilEmail.getEditText().getText().toString().trim();
            String password = binding.tilPassword.getEditText().getText().toString().trim();

            if (email.isEmpty()) {
                binding.tilEmail.setError("Email is required");
                binding.tilEmail.requestFocus();
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.setError("Please provide a valid email");
                binding.tilEmail.requestFocus();
            } else if (password.isEmpty()) {
                binding.tilPassword.setError("Password is required");
                binding.tilPassword.requestFocus();
            } else if (password.length() < 6) {
                binding.tilPassword.setError("Password should be at least 6 characters");
                binding.tilPassword.requestFocus();
            } else {
                binding.tilEmail.setError(null);
                binding.tilPassword.setError(null);
                binding.btnSignIn.setEnabled(false);

                mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            saveCredentials(email, password);
                            checkUserRole(mAuth.getCurrentUser());
                        } else {
                            binding.btnSignIn.setEnabled(true);
                            Toast.makeText(SignIn.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        binding.tvSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SignIn.this, SignUp.class);
            startActivity(intent);
            finish();
        });

        binding.tvForgot.setOnClickListener(v -> {
            Intent intent = new Intent(SignIn.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void loadSavedCredentials() {
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        binding.cbRemember.setChecked(rememberMe);
        if (rememberMe) {
            String email = sharedPreferences.getString(KEY_EMAIL, "");
            String password = sharedPreferences.getString(KEY_PASSWORD, "");
            binding.tilEmail.getEditText().setText(email);
            binding.tilPassword.getEditText().setText(password);
        }
    }

    private void saveCredentials(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (binding.cbRemember.isChecked()) {
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
            editor.putBoolean(KEY_REMEMBER_ME, true);
        } else {
            editor.clear();
        }
        editor.apply();
    }

    private void checkUserRole(FirebaseUser user) {
        if (user == null) return;

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String status = documentSnapshot.getString("status");
                        if ("Deactive".equalsIgnoreCase(status) || "Deactivated".equalsIgnoreCase(status)) {
                            mAuth.signOut();
                            binding.btnSignIn.setEnabled(true);
                            Toast.makeText(SignIn.this, "Your account is deactivated. Please contact support.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        String role = documentSnapshot.getString("role");
                        if ("customer".equals(role)) {
                            Intent intent = new Intent(SignIn.this, MainHome.class);
                            startActivity(intent);
                            finish();
                        } else {
                            mAuth.signOut();
                            binding.btnSignIn.setEnabled(true);
                            Toast.makeText(SignIn.this, "Sellers cannot login as customers", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        mAuth.signOut();
                        binding.btnSignIn.setEnabled(true);
                        Toast.makeText(SignIn.this, "User profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    mAuth.signOut();
                    binding.btnSignIn.setEnabled(true);
                    Toast.makeText(SignIn.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupTextWatchers() {
        binding.tilEmail.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilEmail.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.tilPassword.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilPassword.setError(null);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}

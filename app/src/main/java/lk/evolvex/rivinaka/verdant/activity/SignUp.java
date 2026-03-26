package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.evolvex.rivinaka.verdant.databinding.ActivitySignUpBinding;
import lk.evolvex.rivinaka.verdant.model.User;

public class SignUp extends AppCompatActivity {

    private ActivitySignUpBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        setupTextWatchers();

        binding.tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, SignIn.class);
            startActivity(intent);
            finish();
        });

        binding.tvSellerRegister.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, SellerSignUp.class);
            startActivity(intent);
        });

        binding.btnSignUp.setOnClickListener(v -> {
            registerUser();
        });
    }

    private void registerUser() {
        String fullName = binding.tilFullName.getEditText().getText().toString().trim();
        String email = binding.tilEmail.getEditText().getText().toString().trim();
        String password = binding.tilPassword.getEditText().getText().toString().trim();
        String confirmPassword = binding.tilConfirmPassword.getEditText().getText().toString().trim();

        if (fullName.isEmpty()) {
            binding.tilFullName.setError("Full name is required");
            binding.tilFullName.requestFocus();
        } else if (email.isEmpty()) {
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
        } else if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            binding.tilConfirmPassword.requestFocus();
        } else if (!binding.cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the terms and conditions", Toast.LENGTH_SHORT).show();
        } else {
            binding.tilFullName.setError(null);
            binding.tilEmail.setError(null);
            binding.tilPassword.setError(null);
            binding.tilConfirmPassword.setError(null);

            binding.btnSignUp.setEnabled(false);
            
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                saveUserToFirestore(firebaseUser.getUid(), fullName, email);
                            }
                        } else {
                            binding.btnSignUp.setEnabled(true);
                            Toast.makeText(SignUp.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void saveUserToFirestore(String uid, String fullName, String email) {
        User user = User.builder()
                .userId(uid)
                .fullName(fullName)
                .email(email)
                .role("customer")
                .biometricEnabled(false)
                .createdAt(com.google.firebase.Timestamp.now())
                .build();

        firebaseFirestore.collection("users")
                .document(uid)
                .set(user)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(SignUp.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(SignUp.this, SignIn.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    binding.btnSignUp.setEnabled(true);
                    Toast.makeText(SignUp.this, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupTextWatchers() {
        binding.tilFullName.getEditText().addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilFullName.setError(null);
            }
        });

        binding.tilEmail.getEditText().addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilEmail.setError(null);
            }
        });

        binding.tilPassword.getEditText().addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilPassword.setError(null);
            }
        });

        binding.tilConfirmPassword.getEditText().addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilConfirmPassword.setError(null);
            }
        });
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }
}

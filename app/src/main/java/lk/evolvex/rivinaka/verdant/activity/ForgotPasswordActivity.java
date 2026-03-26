package lk.evolvex.rivinaka.verdant.activity;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.evolvex.rivinaka.verdant.databinding.ActivityForgotPasswordBinding;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean isSeller = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        isSeller = getIntent().getBooleanExtra("isSeller", false);

        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnContinue.setOnClickListener(v -> {
            validateAndSendResetEmail();
        });
    }

    private void validateAndSendResetEmail() {
        String email = binding.tilEmail.getEditText().getText().toString().trim();

        if (email.isEmpty()) {
            binding.tilEmail.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Please enter a valid email");
            return;
        }

        binding.tilEmail.setError(null);

        // Check if user exists in Firestore with correct role
        String role = isSeller ? "seller" : "customer";
        
        db.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("role", role)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // User exists, send Firebase password reset email
                        sendFirebaseResetEmail(email);
                    } else {
                        String message = isSeller ? "No seller account found with this email" : "No customer account found with this email";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendFirebaseResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "A reset link has been sent to your email.", Toast.LENGTH_LONG).show();
                        finish(); // Return to Login screen
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

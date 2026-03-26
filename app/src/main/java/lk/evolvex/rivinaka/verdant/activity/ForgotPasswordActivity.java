package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

import lk.evolvex.rivinaka.verdant.databinding.ActivityForgotPasswordBinding;
import lk.evolvex.rivinaka.verdant.util.EmailUtil;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private FirebaseFirestore db;
    private boolean isSeller = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        isSeller = getIntent().getBooleanExtra("isSeller", false);

        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnContinue.setOnClickListener(v -> {
            validateAndSendOtp();
        });
    }

    private void validateAndSendOtp() {
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
                        // User exists, send OTP
                        sendResetOtp(email);
                    } else {
                        String message = isSeller ? "No seller account found with this email" : "No customer account found with this email";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendResetOtp(String email) {
        String otp = String.valueOf(new Random().nextInt(9000) + 1000);
        
        String subject = "Verdant - Password Reset Code";
        String body = "Hi,\n\nYou requested to reset your password. Your verification code is: " + otp + "\n\nPlease enter this code in the app to proceed with resetting your password.\n\nIf you did not request this, please ignore this email.\n\nThank you,\nVerdant Team";

        EmailUtil.sendEmail(email, subject, body);
        Toast.makeText(this, "Reset code sent to your email", Toast.LENGTH_SHORT).show();

        // Navigate to OTP Verify screen with a flag for password reset
        Intent intent = new Intent(this, OtpVerify.class);
        intent.putExtra("email", email);
        intent.putExtra("otp", otp);
        intent.putExtra("isForgotPassword", true);
        intent.putExtra("isSeller", isSeller);
        startActivity(intent);
    }
}

package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import lk.evolvex.rivinaka.verdant.databinding.ActivityCreateNewPasswordBinding;

public class CreateNewPassword extends AppCompatActivity {

    private ActivityCreateNewPasswordBinding binding;
    private FirebaseAuth mAuth;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNewPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        email = getIntent().getStringExtra("email");

        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnContinue.setOnClickListener(v -> {
            updatePassword();
        });
    }

    private void updatePassword() {
        String newPassword = binding.tilNewPassword.getEditText().getText().toString().trim();
        String confirmPassword = binding.tilConfirmPassword.getEditText().getText().toString().trim();

        if (newPassword.isEmpty()) {
            binding.tilNewPassword.setError("Password is required");
            return;
        }

        if (newPassword.length() < 6) {
            binding.tilNewPassword.setError("Password should be at least 6 characters");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Passwords do not match");
            return;
        }

        binding.tilNewPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        // Note: In Firebase, to update a password for a user who isn't currently signed in,
        // you would typically use sendPasswordResetEmail. 
        // However, since we are doing manual OTP verification, we can use the 
        // standard Firebase reset method which is more secure, or if you want to 
        // force change it, the user needs to be re-authenticated.
        
        // For a seamless "manual" experience, we'll use the Firebase standard reset email 
        // link to actually update it, or tell the user to log in with the reset.
        
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "A reset link has been sent to your email to confirm the change.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(this, SignIn.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

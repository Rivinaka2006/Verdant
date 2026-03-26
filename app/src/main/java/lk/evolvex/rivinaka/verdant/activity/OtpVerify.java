package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;
import java.util.Random;

import lk.evolvex.rivinaka.verdant.databinding.ActivityOtpVerifyBinding;
import lk.evolvex.rivinaka.verdant.model.User;
import lk.evolvex.rivinaka.verdant.util.EmailUtil;

public class OtpVerify extends AppCompatActivity {

    private ActivityOtpVerifyBinding binding;
    private String receivedOtp;
    private String fullName, email, password;
    private boolean isForgotPassword = false;
    private boolean isSeller = false;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOtpVerifyBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        // Get data from Intent
        fullName = getIntent().getStringExtra("fullName");
        email = getIntent().getStringExtra("email");
        password = getIntent().getStringExtra("password");
        receivedOtp = getIntent().getStringExtra("otp");
        isForgotPassword = getIntent().getBooleanExtra("isForgotPassword", false);
        isSeller = getIntent().getBooleanExtra("isSeller", false);

        binding.tvEmail.setText(email);

        setupOtpInputs();
        startResendTimer();

        binding.ivBack.setOnClickListener(v -> finish());

        binding.btnVerify.setOnClickListener(v -> verifyOtp());
    }

    private void setupOtpInputs() {
        binding.etOtp1.addTextChangedListener(new OtpTextWatcher(binding.etOtp1, binding.etOtp2));
        binding.etOtp2.addTextChangedListener(new OtpTextWatcher(binding.etOtp2, binding.etOtp3));
        binding.etOtp3.addTextChangedListener(new OtpTextWatcher(binding.etOtp3, binding.etOtp4));
        binding.etOtp4.addTextChangedListener(new OtpTextWatcher(binding.etOtp4, null));
    }

    private void verifyOtp() {
        String enteredOtp = binding.etOtp1.getText().toString() +
                binding.etOtp2.getText().toString() +
                binding.etOtp3.getText().toString() +
                binding.etOtp4.getText().toString();

        if (enteredOtp.length() < 4) {
            Toast.makeText(this, "Please enter complete OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enteredOtp.equals(receivedOtp)) {
            if (isForgotPassword) {
                // Navigate to Create New Password screen
                Intent intent = new Intent(this, CreateNewPassword.class);
                intent.putExtra("email", email);
                intent.putExtra("isSeller", isSeller);
                startActivity(intent);
                finish();
            } else {
                registerUserInFirebase();
            }
        } else {
            Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUserInFirebase() {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = task.getResult().getUser();
                        if (firebaseUser != null) {
                            saveUserToFirestore(firebaseUser.getUid());
                        }
                    } else {
                        Toast.makeText(OtpVerify.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid) {
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
                    Toast.makeText(OtpVerify.this, "Verification Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(OtpVerify.this, SignIn.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(OtpVerify.this, "Error saving user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void startResendTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        binding.tvTimerCount.setOnClickListener(null);

        countDownTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.tvTimerCount.setText(String.format(Locale.getDefault(), "%d s", millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                binding.tvTimerCount.setText("Resend");
                binding.tvTimerCount.setOnClickListener(v -> {
                    resendOtp();
                });
            }
        }.start();
    }

    private void resendOtp() {
        receivedOtp = String.valueOf(new Random().nextInt(9000) + 1000);
        String subject = isForgotPassword ? "Verdant - Password Reset Code" : "Verdant - Email Verification Code";
        String body = "Hi,\n\nYour verification code is: " + receivedOtp + "\n\nPlease enter this code in the app to proceed.\n\nThank you,\nVerdant Team";
        
        EmailUtil.sendEmail(email, subject, body);
        Toast.makeText(this, "New code sent to " + email, Toast.LENGTH_SHORT).show();
        startResendTimer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private class OtpTextWatcher implements TextWatcher {
        private final android.view.View currentView;
        private final android.view.View nextView;

        public OtpTextWatcher(android.view.View currentView, android.view.View nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }
    }
}

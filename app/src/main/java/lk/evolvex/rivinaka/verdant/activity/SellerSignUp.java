package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.Nursery;
import lk.evolvex.rivinaka.verdant.model.User;

public class SellerSignUp extends AppCompatActivity {

    private TextInputEditText etStoreName, etEmail, etPhone, etAddress, etPassword, etConfirmPassword;
    private CheckBox cbTerms;
    private Button btnRegister;
    private TextView tvSignIn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seller_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnRegister.setOnClickListener(v -> registerSeller());

        tvSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(SellerSignUp.this, SellerSignIn.class);
            startActivity(intent);
            finish();
        });
    }

    private void initViews() {
        etStoreName = findViewById(R.id.etStoreName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);
        tvSignIn = findViewById(R.id.tvSignIn);
    }

    private void registerSeller() {
        String storeName = etStoreName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(storeName) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the terms", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    saveSellerToFirestore(authResult.getUser().getUid(), storeName, email, phone, address);
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Registration Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveSellerToFirestore(String uid, String storeName, String email, String phone, String address) {
        // Create User entry
        User sellerUser = User.builder()
                .userId(uid)
                .fullName(storeName)
                .email(email)
                .phone(phone)
                .role("seller")
                .createdAt(Timestamp.now())
                .build();

        // Create Nursery entry
        String nurseryId = db.collection("nurseries").document().getId();
        Nursery nursery = Nursery.builder()
                .nurseryId(nurseryId)
                .nurseryName(storeName)
                .ownerId(uid)
                .phoneNumber(phone)
                .description(address) // Storing the address in the description field for now
                .latitude(0.0)
                .longitude(0.0)
                .businessHours(new HashMap<>())
                .ratingAverage(0.0)
                .totalReviews(0)
                .createdAt(Timestamp.now())
                .time(Timestamp.now())
                .build();

        WriteBatch batch = db.batch();
        batch.set(db.collection("users").document(uid), sellerUser);
        batch.set(db.collection("nurseries").document(nurseryId), nursery);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Seller Registered successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SellerSignUp.this, SellerMainHome.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Error saving profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

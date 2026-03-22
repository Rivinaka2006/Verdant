package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executor;

import lk.evolvex.rivinaka.verdant.R;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        executor = ContextCompat.getMainExecutor(this);

        TextView versionText = findViewById(R.id.versionText);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String version = pInfo.versionName;
            versionText.setText(getString(R.string.version_format, version));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            versionText.setText("");
        }

        // Delay for splash effect
        new Handler().postDelayed(this::checkUserAuth, 1500);
    }

    private void checkUserAuth() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String status = documentSnapshot.getString("status");
                            if ("Deactive".equalsIgnoreCase(status) || "Deactivated".equalsIgnoreCase(status)) {
                                mAuth.signOut();
                                Toast.makeText(MainActivity.this, "Your account is deactivated. Please contact support.", Toast.LENGTH_LONG).show();
                                navigateToWelcome();
                                return;
                            }

                            Boolean biometricEnabled = documentSnapshot.getBoolean("biometricEnabled");
                            if (biometricEnabled != null && biometricEnabled) {
                                showBiometricPrompt(documentSnapshot.getString("role"));
                            } else {
                                navigateBasedOnRole(documentSnapshot.getString("role"));
                            }
                        } else {
                            navigateToWelcome();
                        }
                    })
                    .addOnFailureListener(e -> navigateToWelcome());
        } else {
            navigateToWelcome();
        }
    }

    private void showBiometricPrompt(String role) {
        BiometricManager biometricManager = BiometricManager.from(this);
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            
            biometricPrompt = new BiometricPrompt(MainActivity.this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(MainActivity.this, "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    navigateToWelcome();
                }

                @Override
                public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);
                    navigateBasedOnRole(role);
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(MainActivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                }
            });

            promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Login")
                    .setSubtitle("Log in using your biometric credential")
                    .setNegativeButtonText("Cancel")
                    .build();

            biometricPrompt.authenticate(promptInfo);
        } else {
            // Biometric not available but enabled in DB, fallback to normal login
            navigateBasedOnRole(role);
        }
    }

    private void navigateBasedOnRole(String role) {
        if ("seller".equals(role)) {
            startActivity(new Intent(MainActivity.this, SellerMainHome.class));
        } else {
            startActivity(new Intent(MainActivity.this, MainHome.class));
        }
        finish();
    }

    private void navigateToWelcome() {
        startActivity(new Intent(MainActivity.this, Welcome.class));
        finish();
    }
}

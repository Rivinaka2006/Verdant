package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.evolvex.rivinaka.verdant.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserAuth();
            }
        }, 1500);
    }

    private void checkUserAuth() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is signed in, fetch status and role from Firestore
            FirebaseFirestore.getInstance().collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String status = documentSnapshot.getString("status");
                            if ("Deactive".equalsIgnoreCase(status) || "Deactivated".equalsIgnoreCase(status)) {
                                FirebaseAuth.getInstance().signOut();
                                Toast.makeText(MainActivity.this, "Your account is deactivated. Please contact support.", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(MainActivity.this, Welcome.class));
                                finish();
                                return;
                            }

                            String role = documentSnapshot.getString("role");
                            if ("seller".equals(role)) {
                                startActivity(new Intent(MainActivity.this, SellerMainHome.class));
                            } else {
                                startActivity(new Intent(MainActivity.this, MainHome.class));
                            }
                        } else {
                            // Document doesn't exist, maybe user deleted or something went wrong
                            startActivity(new Intent(MainActivity.this, Welcome.class));
                        }
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // On failure, default to Welcome or Sign In
                        startActivity(new Intent(MainActivity.this, Welcome.class));
                        finish();
                    });
        } else {
            // No user is signed in, redirect to Welcome
            startActivity(new Intent(MainActivity.this, Welcome.class));
            finish();
        }
    }
}

package lk.evolvex.rivinaka.verdant.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.databinding.ActivityNavMainBinding;
import lk.evolvex.rivinaka.verdant.databinding.NavHeaderMainBinding;
import lk.evolvex.rivinaka.verdant.fragment.CartFragment;
import lk.evolvex.rivinaka.verdant.fragment.HomeFragment;
import lk.evolvex.rivinaka.verdant.fragment.OrdersFragment;
import lk.evolvex.rivinaka.verdant.fragment.ProfileFragment;
import lk.evolvex.rivinaka.verdant.model.User;

public class MainHome extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener,
        NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;
    private ImageView btnDrawer, ivProfilePic;
    private ConstraintLayout headerContainer;
    private TextView tvGreeting, tvUsername;
    private ActivityNavMainBinding binding;
    private NavHeaderMainBinding navHeaderMainBinding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);

        binding = ActivityNavMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        View headerView = binding.navView.getHeaderView(0);
        navHeaderMainBinding = NavHeaderMainBinding.bind(headerView);

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        drawerLayout = binding.drawerLayout;
        navigationView = binding.navView;
        bottomNavigationView = findViewById(R.id.bottom_nav);
        btnDrawer = findViewById(R.id.btnDrawer);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        headerContainer = findViewById(R.id.header_container);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvUsername = findViewById(R.id.tvUsername);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        btnDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.setCheckedItem(R.id.nav_drawer_home);
        }

        updateGreeting();
        fetchUserData();
    }

    private void updateGreeting() {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        String greeting;
        if (timeOfDay >= 0 && timeOfDay < 12) {
            greeting = "Good Morning!";
        } else if (timeOfDay >= 12 && timeOfDay < 16) {
            greeting = "Good Afternoon!";
        } else if (timeOfDay >= 16 && timeOfDay < 21) {
            greeting = "Good Evening!";
        } else {
            greeting = "Good Night!";
        }

        if (tvGreeting != null) {
            tvGreeting.setText(greeting);
        }
    }

    private void fetchUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            firebaseFirestore.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(ds -> {

                        if (ds.exists()) {
                            User user = ds.toObject(User.class);
                            if (user != null) {
                                // Load to Nav Header
                                navHeaderMainBinding.headerUserName.setText(user.getFullName());
                                navHeaderMainBinding.headerUserEmail.setText(user.getEmail());

                                Glide.with(MainHome.this)
                                        .load(user.getProfileImageUrl())
                                        .circleCrop()
                                        .placeholder(R.drawable.user)
                                        .into(navHeaderMainBinding.headerProfilePic);

                                // Load to Content Main Top Bar
                                if (tvUsername != null) {
                                    tvUsername.setText(user.getFullName());
                                }

                                if (ivProfilePic != null) {
                                    Glide.with(MainHome.this)
                                            .load(user.getProfileImageUrl())
                                            .circleCrop()
                                            .placeholder(R.drawable.user)
                                            .into(ivProfilePic);
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainHome.this, "Error fetching user data", Toast.LENGTH_SHORT)
                                .show();
                    });
        }
    }


    public void setHeaderVisibility(int visibility) {
        if (headerContainer != null) {
            headerContainer.setVisibility(visibility);
        }
    }

    public void setBottomNavVisibility(int visibility) {
        if (bottomNavigationView != null) {
            bottomNavigationView.setVisibility(visibility);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_drawer_home) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_drawer_cart) {
            bottomNavigationView.setSelectedItemId(R.id.nav_cart);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_drawer_orders) {
            bottomNavigationView.setSelectedItemId(R.id.nav_orders);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_drawer_profile) {
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        } else if (itemId == R.id.nav_drawer_logout) {
            logout();
            return true;
        }

        Fragment fragment = null;
        if (itemId == R.id.nav_home) {
            fragment = new HomeFragment();
            headerContainer.setVisibility(View.VISIBLE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            navigationView.setCheckedItem(R.id.nav_drawer_home);
        } else if (itemId == R.id.nav_cart) {
            fragment = new CartFragment();
            headerContainer.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            navigationView.setCheckedItem(R.id.nav_drawer_cart);
        } else if (itemId == R.id.nav_orders) {
            fragment = new OrdersFragment();
            headerContainer.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            navigationView.setCheckedItem(R.id.nav_drawer_orders);
        } else if (itemId == R.id.nav_profile) {
            fragment = new ProfileFragment();
            headerContainer.setVisibility(View.GONE);
            bottomNavigationView.setVisibility(View.VISIBLE);
            navigationView.setCheckedItem(R.id.nav_drawer_profile);
        }

        if (fragment != null) {
            loadFragment(fragment);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainHome.this, SignIn.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }
}

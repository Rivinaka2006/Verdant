package lk.evolvex.rivinaka.verdant.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.fragment.CartFragment;
import lk.evolvex.rivinaka.verdant.fragment.HomeFragment;
import lk.evolvex.rivinaka.verdant.fragment.OrdersFragment;
import lk.evolvex.rivinaka.verdant.fragment.ProfileFragment;

public class MainHome extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener, NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;
    private ImageView btnDrawer;
    private ConstraintLayout headerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_main);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        bottomNavigationView = findViewById(R.id.bottom_nav);
        btnDrawer = findViewById(R.id.btnDrawer);
        headerContainer = findViewById(R.id.header_container);

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
            // Optional: You might want to re-evaluate visibility here depending on fragment
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        // 1. Sync: If a side drawer item is clicked, update the Bottom Navigation selection.
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
        }

        // 2. Handle selection logic for Bottom Navigation items
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

    private void loadFragment(Fragment fragment) {
        // Clear backstack when switching main tabs
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }
}

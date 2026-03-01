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
        setContentView(R.layout.activity_main_home);

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

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();
        if (itemId == R.id.nav_home || itemId == R.id.nav_drawer_home) {
            fragment = new HomeFragment();
            headerContainer.setVisibility(View.VISIBLE);
        } else if (itemId == R.id.nav_cart || itemId == R.id.nav_drawer_cart) {
            fragment = new CartFragment();
            headerContainer.setVisibility(View.GONE);
        } else if (itemId == R.id.nav_orders || itemId == R.id.nav_drawer_orders) {
            fragment = new OrdersFragment();
            headerContainer.setVisibility(View.GONE);
        } else if (itemId == R.id.nav_profile || itemId == R.id.nav_drawer_profile) {
            fragment = new ProfileFragment();
            headerContainer.setVisibility(View.GONE);
        }

        if (fragment != null) {
            loadFragment(fragment);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment).commit();
    }
}

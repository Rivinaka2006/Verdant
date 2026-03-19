package lk.evolvex.rivinaka.verdant.activity;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.fragment.SellerDashboardFragment;
import lk.evolvex.rivinaka.verdant.fragment.SellerProductManagementFragment;
import lk.evolvex.rivinaka.verdant.fragment.SellerOrdersFragment;
import lk.evolvex.rivinaka.verdant.fragment.SellerAnalyticsFragment;
import lk.evolvex.rivinaka.verdant.fragment.SellerProfileFragment;

public class SellerMainHome extends AppCompatActivity implements BottomNavigationView.OnItemSelectedListener {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_main_home);

        bottomNavigationView = findViewById(R.id.bottom_nav_seller);
        bottomNavigationView.setOnItemSelectedListener(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        if (savedInstanceState == null) {
            loadFragment(new SellerDashboardFragment());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        int itemId = item.getItemId();

        if (itemId == R.id.nav_seller_dashboard) {
            fragment = new SellerDashboardFragment();
        } else if (itemId == R.id.nav_seller_products) {
            fragment = new SellerProductManagementFragment();
        } else if (itemId == R.id.nav_seller_orders) {
            fragment = new SellerOrdersFragment();
        } else if (itemId == R.id.nav_seller_analytics) {
            fragment = new SellerAnalyticsFragment();
        } else if (itemId == R.id.nav_seller_profile) {
            fragment = new SellerProfileFragment();
        }

        if (fragment != null) {
            loadFragment(fragment);
            return true;
        }
        return false;
    }

    public void selectTab(int itemId) {
        bottomNavigationView.setSelectedItemId(itemId);
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_seller, fragment)
                .commit();
    }
}
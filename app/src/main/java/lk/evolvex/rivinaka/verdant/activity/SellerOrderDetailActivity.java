package lk.evolvex.rivinaka.verdant.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import lk.evolvex.rivinaka.verdant.R;

public class SellerOrderDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seller_order_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_scroll_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup Status Dropdown
        String[] statuses = {"New", "Processing", "Out for Delivery", "Completed", "Cancelled"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statuses);
        AutoCompleteTextView statusDropdown = findViewById(R.id.spinnerStatus);
        statusDropdown.setAdapter(adapter);
    }
}
package lk.evolvex.rivinaka.verdant.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Order;

public class SellerOrderDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private Order currentOrder;
    private String orderId;
    private AutoCompleteTextView statusDropdown;
    private MaterialButton btnUpdateStatus;
    private TextView tvCustomerName, tvCustomerEmail, tvAddress, tvTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seller_order_detail);

        db = FirebaseFirestore.getInstance();
        orderId = getIntent().getStringExtra("orderId");

        initViews();
        setupStatusDropdown();

        if (orderId != null) {
            loadOrderDetails();
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_scroll_view), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnUpdateStatus.setOnClickListener(v -> updateOrderStatus());
    }

    private void initViews() {
        statusDropdown = findViewById(R.id.spinnerStatus);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerEmail = findViewById(R.id.tvCustomerEmail);
        tvAddress = findViewById(R.id.tvAddress);
        tvTotal = findViewById(R.id.tvTotal);
    }

    private void setupStatusDropdown() {
        String[] statuses = {"Pending", "Processing", "Out for Delivery", "Delivered", "Cancelled"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, statuses);
        statusDropdown.setAdapter(adapter);
    }

    private void loadOrderDetails() {
        db.collection("orders").document(orderId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentOrder = documentSnapshot.toObject(Order.class);
                        if (currentOrder != null) {
                            displayOrderDetails();
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load order", Toast.LENGTH_SHORT).show());
    }

    private void displayOrderDetails() {
        tvAddress.setText(currentOrder.getAddress());
        tvTotal.setText(String.format("Total: Rs. %.2f", currentOrder.getTotalAmount()));
        statusDropdown.setText(currentOrder.getStatus(), false);
        
        // Note: Customer details might need a separate fetch if not in Order model
        // For now, using placeholders or data if available
    }

    private void updateOrderStatus() {
        String newStatus = statusDropdown.getText().toString();
        if (currentOrder == null || newStatus.equals(currentOrder.getStatus())) return;

        WriteBatch batch = db.batch();
        DocumentReference orderRef = db.collection("orders").document(orderId);
        batch.update(orderRef, "status", newStatus);

        // If order is being cancelled, restock items
        if ("Cancelled".equalsIgnoreCase(newStatus) && !"Cancelled".equalsIgnoreCase(currentOrder.getStatus())) {
            for (CartItem item : currentOrder.getItems()) {
                DocumentReference productRef = db.collection("products").document(item.getProductId());
                batch.update(productRef, "stock", FieldValue.increment(item.getQuantity()));
            }
        } 
        // If order was cancelled but is now being re-opened (unlikely but handled), deduct stock again
        else if (!"Cancelled".equalsIgnoreCase(newStatus) && "Cancelled".equalsIgnoreCase(currentOrder.getStatus())) {
            for (CartItem item : currentOrder.getItems()) {
                DocumentReference productRef = db.collection("products").document(item.getProductId());
                batch.update(productRef, "stock", FieldValue.increment(-item.getQuantity()));
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    currentOrder.setStatus(newStatus);
                    Toast.makeText(this, "Order updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}

package lk.evolvex.rivinaka.verdant.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.adapter.CheckoutAdapter;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Order;
import lk.evolvex.rivinaka.verdant.model.User;

public class CheckoutActivity extends AppCompatActivity {

    private ImageView btnBack, ivPaymentArrow, ivPaymentIcon;
    private RecyclerView rvOrderList;
    private TextView tvAddressDetails, tvTotalPrice, tvShippingType, tvPaymentMethod;
    private LinearLayout llPaymentHeader, llPaymentOptions;
    private RadioGroup rgPaymentMethods;
    private MaterialButton btnConfirmOrder;
    private CheckoutAdapter adapter;
    private List<CartItem> cartItems;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private double totalAmount = 0;
    private final double SHIPPING_FEE = 400.00;
    private User currentUser;
    private boolean isPaymentExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        loadUserData();
        loadCartItems();
        setupPaymentSelection();

        btnConfirmOrder.setOnClickListener(v -> handleOrderPlacement());
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvOrderList = findViewById(R.id.rvOrderList);
        tvAddressDetails = findViewById(R.id.tvAddressDetails);
        tvTotalPrice = findViewById(R.id.tvTotalPrice);
        tvShippingType = findViewById(R.id.tvShippingType);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        llPaymentHeader = findViewById(R.id.llPaymentHeader);
        llPaymentOptions = findViewById(R.id.llPaymentOptions);
        ivPaymentArrow = findViewById(R.id.ivPaymentArrow);
        ivPaymentIcon = findViewById(R.id.ivPaymentIcon);
        rgPaymentMethods = findViewById(R.id.rgPaymentMethods);
        btnConfirmOrder = findViewById(R.id.btnConfirmOrder);

        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        cartItems = new ArrayList<>();
        adapter = new CheckoutAdapter(cartItems);
        rvOrderList.setLayoutManager(new LinearLayoutManager(this));
        rvOrderList.setAdapter(adapter);
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUser = documentSnapshot.toObject(User.class);
                        if (currentUser != null) {
                            String address = documentSnapshot.getString("address");
                            if (address != null && !address.isEmpty()) {
                                tvAddressDetails.setText(address);
                            }
                        }
                    }
                });
    }

    private void loadCartItems() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("cart")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    cartItems.clear();
                    double subtotal = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        CartItem item = document.toObject(CartItem.class);
                        cartItems.add(item);
                        subtotal += item.getProductPrice() * item.getQuantity();
                    }
                    adapter.notifyDataSetChanged();
                    totalAmount = subtotal + SHIPPING_FEE;
                    tvTotalPrice.setText(String.format("Rs. %.2f", totalAmount));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load cart", Toast.LENGTH_SHORT).show());
    }

    private void setupPaymentSelection() {
        llPaymentHeader.setOnClickListener(v -> {
            isPaymentExpanded = !isPaymentExpanded;
            llPaymentOptions.setVisibility(isPaymentExpanded ? View.VISIBLE : View.GONE);
            ivPaymentArrow.setRotation(isPaymentExpanded ? 180 : 0);
        });

        rgPaymentMethods.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = findViewById(checkedId);
            if (rb != null) {
                String selectedMethod = rb.getText().toString();
                tvPaymentMethod.setText(selectedMethod);
                
                // Update icon based on selection if needed
                if (checkedId == R.id.rbCard) {
                    ivPaymentIcon.setImageResource(android.R.drawable.ic_menu_save); // Replace with card icon if available
                } else {
                    ivPaymentIcon.setImageResource(android.R.drawable.ic_menu_save); // Replace with COD icon if available
                }
                
                // Automatically collapse after selection
                isPaymentExpanded = false;
                llPaymentOptions.setVisibility(View.GONE);
                ivPaymentArrow.setRotation(0);
            }
        });
    }

    private void handleOrderPlacement() {
        String address = tvAddressDetails.getText().toString();
        if (address.equals("Please add a shipping address")) {
            Toast.makeText(this, "Please set a shipping address in your profile", Toast.LENGTH_SHORT).show();
            return;
        }

        String method = tvPaymentMethod.getText().toString();
        if (method.equals("Card Payment")) {
            // Card payment integration will be handled here
            Toast.makeText(this, "Card Payment selected. Integration pending.", Toast.LENGTH_SHORT).show();
        } else {
            placeOrder("Unpaid (COD)");
        }
    }

    private void placeOrder(String paymentStatus) {
        if (mAuth.getCurrentUser() == null) return;
        if (cartItems.isEmpty()) {
            Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String orderId = UUID.randomUUID().toString();

        Order order = Order.builder()
                .orderId(orderId)
                .userId(userId)
                .items(new ArrayList<>(cartItems))
                .totalAmount(totalAmount)
                .shippingFee(SHIPPING_FEE)
                .address(tvAddressDetails.getText().toString())
                .paymentMethod(tvPaymentMethod.getText().toString() + " - " + paymentStatus)
                .status("Pending")
                .createdAt(Timestamp.now())
                .build();

        WriteBatch batch = db.batch();
        DocumentReference orderRef = db.collection("orders").document(orderId);
        batch.set(orderRef, order);

        for (CartItem item : cartItems) {
            DocumentReference cartItemRef = db.collection("users").document(userId)
                    .collection("cart").document(item.getProductId());
            batch.delete(cartItemRef);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e("CheckoutActivity", "Error placing order", e);
                    Toast.makeText(this, "Failed to place order. Try again.", Toast.LENGTH_SHORT).show();
                });
    }
}

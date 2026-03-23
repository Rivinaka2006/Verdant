package lk.evolvex.rivinaka.verdant.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.CheckoutAdapter;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Order;
import lk.evolvex.rivinaka.verdant.model.User;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class CheckoutFragment extends Fragment {

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
    private double subtotal = 0;
    private final double SHIPPING_FEE = 400.00;
    private final double COD_FEE = 100.00;
    private User currentUser;
    private boolean isPaymentExpanded;
    private boolean paymentActive;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_checkout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerView();
        loadUserData();
        loadCartItems();
        setupPaymentSelection(view);

        btnConfirmOrder.setOnClickListener(v -> handleOrderPlacement());
    }

    private void initViews(View view) {
        btnBack = view.findViewById(R.id.btnBack);
        rvOrderList = view.findViewById(R.id.rvOrderList);
        tvAddressDetails = view.findViewById(R.id.tvAddressDetails);
        tvTotalPrice = view.findViewById(R.id.tvTotalPrice);
        tvShippingType = view.findViewById(R.id.tvShippingType);
        tvPaymentMethod = view.findViewById(R.id.tvPaymentMethod);
        llPaymentHeader = view.findViewById(R.id.llPaymentHeader);
        llPaymentOptions = view.findViewById(R.id.llPaymentOptions);
        ivPaymentArrow = view.findViewById(R.id.ivPaymentArrow);
        ivPaymentIcon = view.findViewById(R.id.ivPaymentIcon);
        rgPaymentMethods = view.findViewById(R.id.rgPaymentMethods);
        btnConfirmOrder = view.findViewById(R.id.btnConfirmOrder);

        btnBack.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });
    }

    private void setupRecyclerView() {
        cartItems = new ArrayList<>();
        adapter = new CheckoutAdapter(cartItems);
        rvOrderList.setLayoutManager(new LinearLayoutManager(getContext()));
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
                    subtotal = 0;
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        CartItem item = document.toObject(CartItem.class);
                        cartItems.add(item);
                        subtotal += item.getProductPrice() * item.getQuantity();
                    }
                    adapter.notifyDataSetChanged();
                    updateTotal();
                    paymentActive = true;
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load cart", Toast.LENGTH_SHORT).show());
    }

    private void updateTotal() {
        totalAmount = subtotal + SHIPPING_FEE;
        if (rgPaymentMethods.getCheckedRadioButtonId() == R.id.rbCOD) {
            totalAmount += COD_FEE;
        }
        tvTotalPrice.setText(String.format("Rs. %.2f", totalAmount));
    }

    private void setupPaymentSelection(View view) {
        llPaymentHeader.setOnClickListener(v -> {
            isPaymentExpanded = !isPaymentExpanded;
            llPaymentOptions.setVisibility(isPaymentExpanded ? View.VISIBLE : View.GONE);
            ivPaymentArrow.setRotation(isPaymentExpanded ? 180 : 0);
        });

        rgPaymentMethods.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton rb = view.findViewById(checkedId);
            if (rb != null) {
                String selectedMethod = rb.getText().toString();
                tvPaymentMethod.setText(selectedMethod);

                updateTotal();

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
            Toast.makeText(getContext(), "Please set a shipping address in your profile", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(getContext(), "User data not loaded. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check stock availability and overall availability before proceeding
        validateStockAndProceed();
    }

    private void validateStockAndProceed() {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
        for (CartItem item : cartItems) {
            tasks.add(db.collection("products").document(item.getProductId()).get());
        }

        Tasks.whenAllComplete(tasks).addOnCompleteListener(allTasks -> {
            boolean allAvailable = true;
            StringBuilder issues = new StringBuilder("Please fix following issues before proceeding:\n");

            for (int i = 0; i < tasks.size(); i++) {
                DocumentSnapshot snapshot = tasks.get(i).getResult();
                CartItem cartItem = cartItems.get(i);
                
                if (snapshot.exists()) {
                    Boolean available = snapshot.getBoolean("available");
                    Long stock = snapshot.getLong("stock");
                    
                    if (available != null && !available) {
                        allAvailable = false;
                        issues.append("- ").append(cartItem.getProductName()).append(" is currently unavailable\n");
                    } else if (stock == null || stock < cartItem.getQuantity()) {
                        allAvailable = false;
                        issues.append("- ").append(cartItem.getProductName()).append(" has insufficient quantity\n");
                    }
                } else {
                    allAvailable = false;
                    issues.append("- ").append(cartItem.getProductName()).append(" (Product no longer exists)\n");
                }
            }

            if (allAvailable) {
                proceedToPayment();
            } else {
                Toast.makeText(getContext(), issues.toString(), Toast.LENGTH_LONG).show();
                // Optionally navigate back to cart
            }
        });
    }

    private void proceedToPayment() {
        String method = tvPaymentMethod.getText().toString();
        String generatedOrderId = "V-" + System.currentTimeMillis();

        if (method.equals("Card Payment")) {
            if (paymentActive) {
                InitRequest req = new InitRequest();
                req.setSandBox(true);

                req.setMerchantId("1226871");
                req.setMerchantSecret("NzA3MDEzNjc1MzA0MjQ1Nzc1ODQwNDk2Njg0NTkzMjgxMjk5NTcw");
                req.setCurrency("LKR");
                req.setAmount(totalAmount);
                req.setOrderId(generatedOrderId);
                req.setItemsDescription("Verdant");

                String firstName = currentUser.getFullName();
                String lastName = "";
                if (firstName != null && firstName.contains(" ")) {
                    int lastSpace = firstName.lastIndexOf(" ");
                    lastName = firstName.substring(lastSpace + 1);
                    firstName = firstName.substring(0, lastSpace);
                }

                req.getCustomer().setFirstName(firstName != null ? firstName : "User");
                req.getCustomer().setLastName(lastName.isEmpty() ? "Verdant" : lastName);
                req.getCustomer().setEmail(currentUser.getEmail());
                req.getCustomer().setPhone(currentUser.getPhone() != null ? currentUser.getPhone() : "");

                String address = tvAddressDetails.getText().toString();
                String userAddress = currentUser.getAddress() != null ? currentUser.getAddress() : address;
                req.getCustomer().getAddress().setAddress(userAddress);

                if (currentUser.getShipping() != null) {
                    req.getCustomer().getAddress().setCity(currentUser.getShipping().getCity() != null ? currentUser.getShipping().getCity() : "Colombo");
                    req.getCustomer().getAddress().setCountry(currentUser.getShipping().getCountry() != null ? currentUser.getShipping().getCountry() : "Sri Lanka");
                } else {
                    req.getCustomer().getAddress().setCity("Colombo");
                    req.getCustomer().getAddress().setCountry("Sri Lanka");
                }

                req.setNotifyUrl("https://verdant.requestcatcher.com/test");

                Intent intent = new Intent(getContext(), PHMainActivity.class);
                intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

                payhereLauncher.launch(intent);
            }
        } else {
            placeOrder("Unpaid (COD)");
        }
    }

    private final ActivityResultLauncher<Intent> payhereLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        PHResponse<StatusResponse> response =
                                (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);
                        if (response != null) {
                            if (response.isSuccess()) {
                                Log.i("PAYHERE", "Payment Success");
                                placeOrder("Paid");
                            } else {
                                StatusResponse status = response.getData();
                                if (status != null) {
                                    Log.e("PAYHERE", status.getMessage());
                                    Toast.makeText(getContext(), status.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(getContext(), "Payment cancelled", Toast.LENGTH_SHORT).show();
                }
            });

    private void placeOrder(String paymentStatus) {
        if (mAuth.getCurrentUser() == null) return;
        if (cartItems.isEmpty()) {
            Toast.makeText(getContext(), "Your cart is empty", Toast.LENGTH_SHORT).show();
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

            DocumentReference productRef = db.collection("products").document(item.getProductId());
            batch.update(productRef, "stock", FieldValue.increment(-item.getQuantity()));
            // Increment soldCount when an order is placed
            batch.update(productRef, "soldCount", FieldValue.increment(item.getQuantity()));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Order placed successfully!", Toast.LENGTH_LONG).show();
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("CheckoutFragment", "Error placing order", e);
                    Toast.makeText(getContext(), "Failed to place order. Try again.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainHome) {
            ((MainHome) getActivity()).setBottomNavVisibility(View.VISIBLE);
        }
    }
}

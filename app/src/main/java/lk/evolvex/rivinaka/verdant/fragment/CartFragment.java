package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.CartAdapter;
import lk.evolvex.rivinaka.verdant.model.Address;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.User;

public class CartFragment extends Fragment implements CartAdapter.OnCartItemChangeListener {

    private RecyclerView rvCartItems;
    private TextView tvSubtotal, tvShippingFee, tvTotal, tvEmptyCart, tvItemCount;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private final double SHIPPING_FEE = 400.00;
    private Button btnCheckout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerView();
        loadCartItems();

        btnCheckout.setOnClickListener(v -> {
            if (cartItems.isEmpty()) {
                Toast.makeText(getContext(), "Your cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Check if any items are unavailable
            boolean hasUnavailableItems = false;
            for (CartItem item : cartItems) {
                if (!item.isAvailable()) {
                    hasUnavailableItems = true;
                    break;
                }
            }
            
            if (hasUnavailableItems) {
                Toast.makeText(getContext(), "Please remove unavailable items before checkout", Toast.LENGTH_LONG).show();
                return;
            }
            
            checkAddressesAndCheckout();
        });
    }

    private void initViews(View view) {
        rvCartItems = view.findViewById(R.id.recyclerCart);
        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvShippingFee = view.findViewById(R.id.tvShippingFee);
        tvTotal = view.findViewById(R.id.tvTotal);
        tvEmptyCart = view.findViewById(R.id.tvEmptyCart);
        tvItemCount = view.findViewById(R.id.tvItemCount);
        btnCheckout = view.findViewById(R.id.btnCheckout);
        progressBar = view.findViewById(R.id.progressBarCart);
    }

    private void setupRecyclerView() {
        cartItems = new ArrayList<>();
        cartAdapter = new CartAdapter(cartItems, this);
        rvCartItems.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCartItems.setAdapter(cartAdapter);
    }

    private void loadCartItems() {
        if (mAuth.getCurrentUser() == null) {
            tvEmptyCart.setVisibility(View.VISIBLE);
            tvEmptyCart.setText("Please sign in to see your cart");
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("cart")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CartItem> tempItems = new ArrayList<>();
                    List<Task<DocumentSnapshot>> tasks = new ArrayList<>();
                    
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        CartItem item = document.toObject(CartItem.class);
                        tempItems.add(item);
                        // Fetch latest availability from products collection
                        tasks.add(db.collection("products").document(item.getProductId()).get());
                    }
                    
                    if (tasks.isEmpty()) {
                        cartItems.clear();
                        updateUI();
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        return;
                    }

                    Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
                        cartItems.clear();
                        for (int i = 0; i < tasks.size(); i++) {
                            CartItem item = tempItems.get(i);
                            DocumentSnapshot productDoc = tasks.get(i).getResult();
                            if (productDoc.exists()) {
                                Boolean available = productDoc.getBoolean("available");
                                item.setAvailable(available != null && available);
                                // Also update price and name in case they changed
                                Double price = productDoc.getDouble("price");
                                if (price != null) item.setProductPrice(price);
                                String name = productDoc.getString("name");
                                if (name != null) item.setProductName(name);
                            } else {
                                item.setAvailable(false); // Product no longer exists
                            }
                            cartItems.add(item);
                        }
                        cartAdapter.notifyDataSetChanged();
                        updateUI();
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    });
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load cart", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkAddressesAndCheckout() {
        if (mAuth.getCurrentUser() == null) return;

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (isProfileComplete(user)) {
                            navigateToCheckout();
                        } else {
                            Toast.makeText(getContext(), "Please complete your shipping and billing address in profile", Toast.LENGTH_LONG).show();
                            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_nav);
                            if (bottomNav != null) {
                                bottomNav.setSelectedItemId(R.id.nav_profile);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Error verifying profile information", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToCheckout() {
        if (getActivity() instanceof MainHome) {
            ((MainHome) getActivity()).setBottomNavVisibility(View.GONE);
        }
        
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CheckoutFragment())
                .addToBackStack(null)
                .commit();
    }

    private boolean isProfileComplete(User user) {
        if (user == null || user.getBilling() == null || user.getShipping() == null) {
            return false;
        }

        Address billing = user.getBilling();
        Address shipping = user.getShipping();

        return isAddressValid(billing) && isAddressValid(shipping);
    }

    private boolean isAddressValid(Address address) {
        return address.getAddress() != null && !address.getAddress().trim().isEmpty() &&
                address.getCity() != null && !address.getCity().trim().isEmpty() &&
                address.getFirstName() != null && !address.getFirstName().trim().isEmpty();
    }

    private void updateUI() {
        if (cartItems.isEmpty()) {
            tvEmptyCart.setVisibility(View.VISIBLE);
            rvCartItems.setVisibility(View.GONE);
            tvItemCount.setText("0 items");
        } else {
            tvEmptyCart.setVisibility(View.GONE);
            rvCartItems.setVisibility(View.VISIBLE);
            tvItemCount.setText(cartItems.size() + (cartItems.size() == 1 ? " item" : " items"));
        }
        updateTotals();
    }

    private void updateTotals() {
        double subtotal = 0;
        for (CartItem item : cartItems) {
            if (item.isAvailable()) {
                subtotal += item.getProductPrice() * item.getQuantity();
            }
        }
        
        double total = subtotal > 0 ? subtotal + SHIPPING_FEE : 0;

        tvSubtotal.setText(String.format("Rs. %.2f", subtotal));
        tvShippingFee.setText(String.format("Rs. %.2f", subtotal > 0 ? SHIPPING_FEE : 0));
        tvTotal.setText(String.format("Rs. %.2f", total));
    }

    @Override
    public void onQuantityChanged(CartItem item) {
        updateTotals();
        updateFirestore(item);
    }

    @Override
    public void onItemDeleted(CartItem item) {
        updateUI();
        deleteFromFirestore(item);
    }

    private void updateFirestore(CartItem item) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("cart")
                .document(item.getProductId())
                .set(item)
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to sync quantity", Toast.LENGTH_SHORT).show());
    }

    private void deleteFromFirestore(CartItem item) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).collection("cart")
                .document(item.getProductId())
                .delete()
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to remove item", Toast.LENGTH_SHORT).show());
    }
}

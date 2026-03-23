package lk.evolvex.rivinaka.verdant.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.adapter.OrderAdapter;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Order;

public class OrdersFragment extends Fragment implements OrderAdapter.OnOrderClickListener {

    private RecyclerView rvOrders;
    private OrderAdapter adapter;
    private List<Order> allOrders = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView tvOngoing, tvDelivered, tvCancelled;
    private String currentFilter = "Ongoing";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupRecyclerView();
        loadOrders();
        setupTabs();
    }

    private void initViews(View view) {
        rvOrders = view.findViewById(R.id.rvOrders);
        progressBar = view.findViewById(R.id.progressBar);
        tvOngoing = view.findViewById(R.id.tvOngoing);
        tvDelivered = view.findViewById(R.id.tvCompleted); // Keeping ID same for layout compatibility
        tvCancelled = view.findViewById(R.id.tvCancelled);
        
        // Update text to Delivered
        tvDelivered.setText("Delivered");
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(new ArrayList<>(), this);
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(adapter);
    }

    private void setupTabs() {
        tvOngoing.setOnClickListener(v -> updateFilter("Ongoing"));
        tvDelivered.setOnClickListener(v -> updateFilter("Delivered"));
        tvCancelled.setOnClickListener(v -> updateFilter("Cancelled"));
    }

    private void updateFilter(String filter) {
        currentFilter = filter;
        
        // Reset tab styles
        resetTabStyles();
        
        // Highlight selected tab
        if (filter.equals("Ongoing")) {
            highlightTab(tvOngoing);
        } else if (filter.equals("Delivered")) {
            highlightTab(tvDelivered);
        } else if (filter.equals("Cancelled")) {
            highlightTab(tvCancelled);
        }
        
        applyFilter();
    }

    private void resetTabStyles() {
        int inactiveBg = R.drawable.bg_filter_inactive;
        int inactiveText = ContextCompat.getColor(requireContext(), R.color.app_green);

        tvOngoing.setBackgroundResource(inactiveBg);
        tvOngoing.setTextColor(inactiveText);
        
        tvDelivered.setBackgroundResource(inactiveBg);
        tvDelivered.setTextColor(inactiveText);
        
        tvCancelled.setBackgroundResource(inactiveBg);
        tvCancelled.setTextColor(inactiveText);
    }

    private void highlightTab(TextView tv) {
        tv.setBackgroundResource(R.drawable.bg_filter_active);
        tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
    }

    private void loadOrders() {
        if (mAuth.getCurrentUser() == null) return;

        progressBar.setVisibility(View.VISIBLE);
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("orders")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allOrders.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Order order = document.toObject(Order.class);
                        allOrders.add(order);
                    }
                    applyFilter();
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Failed to load orders", Toast.LENGTH_SHORT).show();
                });
    }

    private void applyFilter() {
        List<Order> filteredList;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            filteredList = allOrders.stream().filter(order -> {
                String status = order.getStatus();
                if (currentFilter.equals("Ongoing")) {
                    return !"Delivered".equalsIgnoreCase(status) && !"Cancelled".equalsIgnoreCase(status) && !"Completed".equalsIgnoreCase(status);
                } else if (currentFilter.equals("Delivered")) {
                    return "Delivered".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status);
                } else if (currentFilter.equals("Cancelled")) {
                    return "Cancelled".equalsIgnoreCase(status);
                }
                return false;
            }).collect(Collectors.toList());
        } else {
            filteredList = new ArrayList<>();
            for (Order order : allOrders) {
                String status = order.getStatus();
                if (currentFilter.equals("Ongoing")) {
                    if (!"Delivered".equalsIgnoreCase(status) && !"Cancelled".equalsIgnoreCase(status) && !"Completed".equalsIgnoreCase(status)) {
                        filteredList.add(order);
                    }
                } else if (currentFilter.equals("Delivered")) {
                    if ("Delivered".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                        filteredList.add(order);
                    }
                } else if (currentFilter.equals("Cancelled")) {
                    if ("Cancelled".equalsIgnoreCase(status)) {
                        filteredList.add(order);
                    }
                }
            }
        }
        adapter.updateList(filteredList);
    }

    @Override
    public void onTrackOrder(Order order) {
        Toast.makeText(getContext(), "Tracking order: " + order.getOrderId(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCancelOrder(Order order) {
        if ("Cancelled".equalsIgnoreCase(order.getStatus()) || "Delivered".equalsIgnoreCase(order.getStatus()) || "Completed".equalsIgnoreCase(order.getStatus())) {
            return;
        }

        WriteBatch batch = db.batch();
        DocumentReference orderRef = db.collection("orders").document(order.getOrderId());
        batch.update(orderRef, "status", "Cancelled");

        // Restock items
        for (CartItem item : order.getItems()) {
            DocumentReference productRef = db.collection("products").document(item.getProductId());
            batch.update(productRef, "stock", FieldValue.increment(item.getQuantity()));
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    order.setStatus("Cancelled");
                    applyFilter();
                    Toast.makeText(getContext(), "Order cancelled and items restocked", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to cancel order", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRateProduct(CartItem item) {
        showRatingDialog(item);
    }

    private void showRatingDialog(CartItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rate_product, null);
        builder.setView(dialogView);

        TextView tvProductName = dialogView.findViewById(R.id.tvProductName);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        MaterialButton btnSubmitRating = dialogView.findViewById(R.id.btnSubmitRating);

        tvProductName.setText(item.getProductName());

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnSubmitRating.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            if (rating == 0) {
                Toast.makeText(getContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            submitRating(item.getProductId(), rating, dialog);
        });

        dialog.show();
    }

    private void submitRating(String productId, float rating, AlertDialog dialog) {
        DocumentReference productRef = db.collection("products").document(productId);

        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot productSnapshot = transaction.get(productRef);
            
            double currentRating = 0.0;
            long currentCount = 0;
            
            if (productSnapshot.exists()) {
                Double ratingVal = productSnapshot.getDouble("rating");
                Long countVal = productSnapshot.getLong("ratingCount");
                
                if (ratingVal != null) currentRating = ratingVal;
                if (countVal != null) currentCount = countVal;
            }

            // Calculate new average rating
            double totalRatingSum = (currentRating * currentCount) + rating;
            long newCount = currentCount + 1;
            double newAverageRating = totalRatingSum / newCount;

            transaction.update(productRef, "rating", newAverageRating);
            transaction.update(productRef, "ratingCount", newCount);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Rating submitted successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to submit rating: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}

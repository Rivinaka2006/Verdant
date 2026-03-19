package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.adapter.SellerOrderAdapter;
import lk.evolvex.rivinaka.verdant.model.Order;

public class SellerOrderListFragment extends Fragment implements SellerOrderAdapter.OnOrderActionClickListener {

    private static final String ARG_STATUS = "order_status";
    private String statusFilter;
    private RecyclerView rvOrders;
    private SellerOrderAdapter adapter;
    private List<Order> orders;
    private ProgressBar pbOrders;
    private TextView tvEmpty;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public static SellerOrderListFragment newInstance(String status) {
        SellerOrderListFragment fragment = new SellerOrderListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_STATUS, status);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            statusFilter = getArguments().getString(ARG_STATUS);
        }
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_order_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvOrders = view.findViewById(R.id.rvSellerOrders);
        pbOrders = view.findViewById(R.id.pbOrders);
        tvEmpty = view.findViewById(R.id.tvEmptyOrders);

        orders = new ArrayList<>();
        adapter = new SellerOrderAdapter(orders, this);
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));
        rvOrders.setAdapter(adapter);

        loadOrders();
    }

    private void loadOrders() {
        if (mAuth.getCurrentUser() == null) return;

        pbOrders.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        Query query = db.collection("orders")
                .whereEqualTo("status", statusFilter)
                .orderBy("createdAt", Query.Direction.DESCENDING);

        query.addSnapshotListener((value, error) -> {
            if (!isAdded()) return;
            pbOrders.setVisibility(View.GONE);
            if (error != null) {
                Log.e("SellerOrderList", "Error loading orders", error);
                return;
            }

            orders.clear();
            if (value != null) {
                for (QueryDocumentSnapshot doc : value) {
                    Order order = doc.toObject(Order.class);
                    orders.add(order);
                }
            }

            adapter.notifyDataSetChanged();
            tvEmpty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onActionClick(Order order, String action) {
        String newStatus = "";
        switch (action) {
            case "Accept Order":
                newStatus = "Processing";
                break;
            case "Cancel":
                newStatus = "Cancelled";
                break;
            case "Mark Shipped":
                newStatus = "Shipped";
                break;
        }

        if (!newStatus.isEmpty()) {
            updateOrderStatus(order.getOrderId(), newStatus);
        }
    }

    private void updateOrderStatus(String orderId, String newStatus) {
        db.collection("orders").document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Order status updated to: " + newStatus, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "Failed to update order status", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

package lk.evolvex.rivinaka.verdant.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.AddEditProductActivity;
import lk.evolvex.rivinaka.verdant.adapter.SellerProductAdapter;
import lk.evolvex.rivinaka.verdant.model.Product;

public class SellerProductManagementFragment extends Fragment implements SellerProductAdapter.OnProductActionListener {

    private RecyclerView rvProducts;
    private FloatingActionButton fabAddProduct;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<Product> productList;
    private SellerProductAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_product_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        rvProducts = view.findViewById(R.id.rvProducts);
        fabAddProduct = view.findViewById(R.id.fabAddProduct);

        fabAddProduct.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddEditProductActivity.class);
            startActivity(intent);
        });

        setupRecyclerView();
        loadSellerProducts();
    }

    private void setupRecyclerView() {
        productList = new ArrayList<>();
        adapter = new SellerProductAdapter(productList, this);
        rvProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvProducts.setAdapter(adapter);
    }

    private void loadSellerProducts() {
        if (mAuth.getCurrentUser() == null) return;

        String sellerId = mAuth.getCurrentUser().getUid();

        db.collection("products")
                .whereEqualTo("nurseryId", sellerId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (var doc : queryDocumentSnapshots) {
                        Product product = doc.toObject(Product.class);
                        product.setProductId(doc.getId());
                        productList.add(product);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading products: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onEditClick(Product product) {
        Intent intent = new Intent(getContext(), AddEditProductActivity.class);
        intent.putExtra("productId", product.getProductId());
        // You can pass more data or handle editing logic in AddEditProductActivity
        startActivity(intent);
    }

    @Override
    public void onToggleAvailability(Product product, boolean isAvailable) {
        db.collection("products").document(product.getProductId())
                .update("available", isAvailable)
                .addOnSuccessListener(aVoid -> {
                    product.setAvailable(isAvailable);
                    Toast.makeText(getContext(), "Availability updated", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    adapter.notifyDataSetChanged(); // Reset switch
                    Toast.makeText(getContext(), "Update failed", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSellerProducts(); // Refresh list when returning from Add/Edit
    }
}

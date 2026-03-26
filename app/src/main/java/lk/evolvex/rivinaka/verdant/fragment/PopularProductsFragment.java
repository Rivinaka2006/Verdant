package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.PopularProductAdapter;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Product;

public class PopularProductsFragment extends Fragment implements PopularProductAdapter.OnProductClickListener {

    private RecyclerView rvPopularProductsAll;
    private PopularProductAdapter adapter;
    private List<Product> productList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Map<String, Integer> soldCountMap = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_popular_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            mainHome.setHeaderVisibility(View.GONE);
            mainHome.setBottomNavVisibility(View.GONE);
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        rvPopularProductsAll = view.findViewById(R.id.rvPopularProductsAll);
        productList = new ArrayList<>();
        adapter = new PopularProductAdapter(productList, this);
        rvPopularProductsAll.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPopularProductsAll.setAdapter(adapter);

        loadSoldCountMapAndProducts();
    }

    private void loadSoldCountMapAndProducts() {
        db.collection("orders")
                .whereIn("status", Arrays.asList("Delivered", "DELIVERED", "Completed", "COMPLETED"))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    soldCountMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) doc.get("items");
                        if (items != null) {
                            for (Map<String, Object> item : items) {
                                String productId = (String) item.get("productId");
                                Long quantity = (Long) item.get("quantity");
                                if (productId != null && quantity != null) {
                                    soldCountMap.put(productId, soldCountMap.getOrDefault(productId, 0) + quantity.intValue());
                                }
                            }
                        }
                    }
                    loadAllPopularProducts();
                })
                .addOnFailureListener(e -> {
                    Log.e("PopularProductsFrag", "Error loading sold counts", e);
                    loadAllPopularProducts();
                });
    }

    private void loadAllPopularProducts() {
        db.collection("products")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setProductId(document.getId());
                        // Apply calculated sold count
                        product.setSoldCount(soldCountMap.getOrDefault(product.getProductId(), 0));
                        productList.add(product);
                    }
                    
                    // Sort locally since we're using dynamic sold counts
                    productList.sort((p1, p2) -> Integer.compare(p2.getSoldCount(), p1.getSoldCount()));
                    
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("PopularProductsFrag", "Error loading popular products", e);
                    Toast.makeText(getContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onProductClick(Product product) {
        Fragment singleProductFragment = new singleProductFragment();
        Bundle bundle = new Bundle();
        bundle.putString("productId", product.getProductId());
        singleProductFragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, singleProductFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onAddToCartClick(Product product) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please sign in to add to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference cartRef = db.collection("users").document(userId)
                .collection("cart").document(product.getProductId());

        cartRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                CartItem existingItem = documentSnapshot.toObject(CartItem.class);
                if (existingItem != null) {
                    existingItem.setQuantity(existingItem.getQuantity() + 1);
                    existingItem.setAvailable(product.isAvailable());
                    cartRef.set(existingItem)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Quantity updated in cart", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update cart", Toast.LENGTH_SHORT).show());
                }
            } else {
                String imageUrl = (product.getImageUrls() != null && !product.getImageUrls().isEmpty())
                        ? product.getImageUrls().get(0) : "";

                CartItem newItem = CartItem.builder()
                        .productId(product.getProductId())
                        .productName(product.getName())
                        .productPrice(product.getPrice())
                        .quantity(1)
                        .productImage(imageUrl)
                        .available(product.isAvailable())
                        .build();

                cartRef.set(newItem)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Added to cart", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to add to cart", Toast.LENGTH_SHORT).show());
            }
        }).addOnFailureListener(e -> {
            Log.e("PopularProductsFrag", "Error checking cart", e);
            Toast.makeText(getContext(), "Failed to access cart", Toast.LENGTH_SHORT).show();
        });
    }
}

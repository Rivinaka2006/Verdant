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
import java.util.List;

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

        loadAllPopularProducts();
    }

    private void loadAllPopularProducts() {
        db.collection("products")
                .whereEqualTo("available", true)
                .orderBy("soldCount", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setProductId(document.getId());
                        productList.add(product);
                    }
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

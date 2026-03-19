package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import lk.evolvex.rivinaka.verdant.adapter.ForYouProductAdapter;
import lk.evolvex.rivinaka.verdant.adapter.PopularProductAdapter;
import lk.evolvex.rivinaka.verdant.adapter.SpecialOfferAdapter;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Product;

public class HomeFragment extends Fragment implements SpecialOfferAdapter.OnProductClickListener, PopularProductAdapter.OnProductClickListener, ForYouProductAdapter.OnProductClickListener {

    private RecyclerView rvSpecialOffers, rvPopularProducts, rvForYou;
    private SpecialOfferAdapter specialOfferAdapter;
    private ForYouProductAdapter forYouAdapter;
    private PopularProductAdapter popularProductAdapter;
    private List<Product> specialOfferList, popularProductList, forYouProductList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout layoutCategories;
    private String selectedCategory = "All";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Ensure the top header and bottom navigation are visible
        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            mainHome.setHeaderVisibility(View.VISIBLE);
            mainHome.setBottomNavVisibility(View.VISIBLE);
        }

        view.findViewById(R.id.tvSeeAllOffers).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SpecialOffersFragment())
                    .addToBackStack(null)
                    .commit();
        });

        view.findViewById(R.id.tvSeeAllPopular).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new PopularProductsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Initialize Special Offers RecyclerView
        rvSpecialOffers = view.findViewById(R.id.rvSpecialOffers);
        specialOfferList = new ArrayList<>();
        specialOfferAdapter = new SpecialOfferAdapter(specialOfferList, this);
        rvSpecialOffers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSpecialOffers.setAdapter(specialOfferAdapter);

        // Initialize Popular Products RecyclerView
        rvPopularProducts = view.findViewById(R.id.rvPopularProducts);
        popularProductList = new ArrayList<>();
        popularProductAdapter = new PopularProductAdapter(popularProductList, this);
        rvPopularProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPopularProducts.setAdapter(popularProductAdapter);

        // Initialize For You RecyclerView
        rvForYou = view.findViewById(R.id.rvForYou);
        forYouProductList = new ArrayList<>();
        forYouAdapter = new ForYouProductAdapter(forYouProductList, this);
        // LayoutManager is set in XML as GridLayoutManager
        rvForYou.setAdapter(forYouAdapter);

        // Initialize Category Filters
        layoutCategories = view.findViewById(R.id.layoutCategories);
        setupCategoryFilters();

        loadSpecialOffers();
        loadPopularProducts();
        loadForYouProducts();
    }

    private void setupCategoryFilters() {
        for (int i = 0; i < layoutCategories.getChildCount(); i++) {
            View child = layoutCategories.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                tv.setOnClickListener(v -> {
                    selectedCategory = tv.getText().toString();
                    updateCategoryUI();
                    loadPopularProducts();
                });
            }
        }
    }

    private void updateCategoryUI() {
        for (int i = 0; i < layoutCategories.getChildCount(); i++) {
            View child = layoutCategories.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if (tv.getText().toString().equals(selectedCategory)) {
                    tv.setBackgroundResource(R.drawable.bg_filter_active);
                    tv.setTextColor(ContextCompat.getColor(getContext(), R.color.text_primary));
                } else {
                    tv.setBackgroundResource(R.drawable.bg_filter_inactive);
                    tv.setTextColor(ContextCompat.getColor(getContext(), R.color.app_green));
                }
            }
        }
    }

    private void loadSpecialOffers() {
        db.collection("products")
                .whereEqualTo("available", true)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    specialOfferList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setProductId(document.getId());
                        specialOfferList.add(product);
                    }
                    specialOfferAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error loading special offers", e);
                    Toast.makeText(getContext(), "Failed to load special offers", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPopularProducts() {
        Query query = db.collection("products")
                .whereEqualTo("available", true);

        if (!selectedCategory.equals("All")) {
            query = query.whereEqualTo("category", selectedCategory);
        }

        query.orderBy("soldCount", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    popularProductList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setProductId(document.getId());
                        popularProductList.add(product);
                    }
                    popularProductAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error loading popular products", e);
                    Toast.makeText(getContext(), "Failed to load popular products", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadForYouProducts() {
        db.collection("products")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    forYouProductList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setProductId(document.getId());
                        forYouProductList.add(product);
                    }
                    forYouAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Error loading For You products", e);
                    Toast.makeText(getContext(), "Failed to load For You products", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onProductClick(Product product) {
        // Navigate to the SingleProductFragment and pass product data if needed
        Fragment singleProductFragment = new singleProductFragment();
        // Bundle can be used to pass product details or ID
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
            Log.e("HomeFragment", "Error checking cart", e);
            Toast.makeText(getContext(), "Failed to access cart", Toast.LENGTH_SHORT).show();
        });
    }
}

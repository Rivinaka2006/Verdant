package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.ImageSliderAdapter;
import lk.evolvex.rivinaka.verdant.adapter.SpecialOfferAdapter;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Product;
import lk.evolvex.rivinaka.verdant.model.Nursery;

public class singleProductFragment extends Fragment implements SpecialOfferAdapter.OnProductClickListener, OnMapReadyCallback {

    private String productId;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private TextView tvProductName, tvProductWeight, tvPrice, tvDescription, tvCareInstructions, tvWateringFrequency, tvLightRequirement, tvRating;
    private ViewPager2 viewPager;
    private Button btnAddToCart, btnBuyNow;
    private Product currentProduct;

    // Seller Details Views
    private ImageView ivSellerProfile;
    private TextView tvSellerName, tvSellerRating;
    private Button btnViewShop;

    // Map
    private MapView mapView;
    private GoogleMap googleMap;
    private Nursery currentNursery;

    // Similar Items
    private RecyclerView rvSimilarItems;
    private SpecialOfferAdapter similarItemsAdapter;
    private List<Product> similarItemsList;

    public singleProductFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_single_product, container, false);
        
        mapView = view.findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews(view);
        setupToolbar(view);

        if (productId != null) {
            loadProductDetails();
        } else {
            Toast.makeText(getContext(), "Product not found", Toast.LENGTH_SHORT).show();
        }

        btnAddToCart.setOnClickListener(v -> {
            if (currentProduct != null) {
                addToCart();
            } else {
                Toast.makeText(getContext(), "Loading product details...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initViews(View view) {
        tvProductName = view.findViewById(R.id.tv_product_name);
        tvProductWeight = view.findViewById(R.id.tv_product_weight);
        tvPrice = view.findViewById(R.id.tv_price);
        tvRating = view.findViewById(R.id.tv_rating);
        tvDescription = view.findViewById(R.id.tv_description_text);
        tvCareInstructions = view.findViewById(R.id.tv_care_instructions_text);
        tvWateringFrequency = view.findViewById(R.id.tv_watering_frequency_text);
        tvLightRequirement = view.findViewById(R.id.tv_light_requirement_text);
        viewPager = view.findViewById(R.id.viewPager_product_media);
        btnAddToCart = view.findViewById(R.id.btn_add_to_cart);
        btnBuyNow = view.findViewById(R.id.btn_buy_now);

        // Seller views
        ivSellerProfile = view.findViewById(R.id.iv_seller_profile);
        tvSellerName = view.findViewById(R.id.tv_seller_name);
        tvSellerRating = view.findViewById(R.id.tv_seller_rating);
        btnViewShop = view.findViewById(R.id.btn_view_shop);

        // Similar Items
        rvSimilarItems = view.findViewById(R.id.rv_similar_items);
        similarItemsList = new ArrayList<>();
        similarItemsAdapter = new SpecialOfferAdapter(similarItemsList, this);
        rvSimilarItems.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSimilarItems.setAdapter(similarItemsAdapter);
    }

    private void setupToolbar(View view) {
        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            mainHome.setHeaderVisibility(View.GONE);
            mainHome.setBottomNavVisibility(View.GONE);
        }

        ImageView btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }
    }

    private void loadProductDetails() {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentProduct = documentSnapshot.toObject(Product.class);
                        if (currentProduct != null) {
                            currentProduct.setProductId(documentSnapshot.getId());
                            displayProductData(currentProduct);
                            
                            // Load nursery/seller details
                            if (currentProduct.getNurseryId() != null && !currentProduct.getNurseryId().isEmpty()) {
                                loadSellerDetails(currentProduct.getNurseryId());
                            }

                            // Load similar items based on category
                            loadSimilarItems(currentProduct.getCategory());
                        }
                    } else {
                        Toast.makeText(getContext(), "Product details not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("SingleProductFragment", "Error loading product", e);
                    Toast.makeText(getContext(), "Failed to load details", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSimilarItems(String category) {
        if (category == null) return;

        db.collection("products")
                .whereEqualTo("category", category)
                .whereEqualTo("available", true)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    similarItemsList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setProductId(document.getId());
                        
                        // Don't show the current product in similar items
                        if (!product.getProductId().equals(productId)) {
                            similarItemsList.add(product);
                        }
                    }
                    similarItemsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("SingleProductFragment", "Error loading similar items", e));
    }

    private void loadSellerDetails(String id) {
        db.collection("nurseries").document(id).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Nursery nursery = documentSnapshot.toObject(Nursery.class);
                        if (nursery != null) {
                            nursery.setNurseryId(documentSnapshot.getId());
                            displaySellerData(nursery);
                        }
                    } else {
                        db.collection("nurseries").whereEqualTo("ownerId", id).get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        Nursery nursery = queryDocumentSnapshots.getDocuments().get(0).toObject(Nursery.class);
                                        if (nursery != null) {
                                            nursery.setNurseryId(queryDocumentSnapshots.getDocuments().get(0).getId());
                                            displaySellerData(nursery);
                                        }
                                    }
                                });
                    }
                });
    }

    private void displayProductData(Product product) {
        tvProductName.setText(product.getName());
        tvProductWeight.setText(product.getCategory());
        tvPrice.setText("Rs. " + product.getPrice());
        tvDescription.setText(product.getDescription());
        tvCareInstructions.setText(product.getCareInstructions());
        tvWateringFrequency.setText(product.getWaterFrequency());
        tvLightRequirement.setText(product.getLightRequirement());

        // Display rating and review count
        tvRating.setText(String.format(Locale.getDefault(), "%.1f (%d reviews)", 
                product.getRating(), product.getRatingCount()));

        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            ImageSliderAdapter adapter = new ImageSliderAdapter(product.getImageUrls());
            viewPager.setAdapter(adapter);
        }
        
        if (!product.isAvailable()) {
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Unavailable");
            btnBuyNow.setEnabled(false);
        } else {
            btnAddToCart.setEnabled(true);
            btnAddToCart.setText("Add to Cart");
            btnBuyNow.setEnabled(true);
        }
    }

    private void displaySellerData(Nursery nursery) {
        if (getActivity() == null) return;
        
        this.currentNursery = nursery;
        tvSellerName.setText(nursery.getNurseryName());
        tvSellerRating.setText(String.format(Locale.getDefault(), "%.1f (%d reviews)", nursery.getRatingAverage(), nursery.getTotalReviews()));
        
        if (nursery.getBannerImageUrl() != null && !nursery.getBannerImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(nursery.getBannerImageUrl())
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(ivSellerProfile);
        }

        btnViewShop.setOnClickListener(v -> {
            ShopDetailsFragment fragment = ShopDetailsFragment.newInstance(nursery.getNurseryId());
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        updateMapLocation();
    }

    private void updateMapLocation() {
        if (googleMap != null && currentNursery != null) {
            LatLng location = new LatLng(currentNursery.getLatitude(), currentNursery.getLongitude());
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(location).title(currentNursery.getNurseryName()));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        updateMapLocation();
    }

    @Override
    public void onProductClick(Product product) {
        // Reload fragment with new product
        Fragment fragment = new singleProductFragment();
        Bundle bundle = new Bundle();
        bundle.putString("productId", product.getProductId());
        fragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void addToCart() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Please sign in to add to cart", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        DocumentReference cartRef = db.collection("users").document(userId)
                .collection("cart").document(productId);

        cartRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                CartItem existingItem = documentSnapshot.toObject(CartItem.class);
                if (existingItem != null) {
                    existingItem.setQuantity(existingItem.getQuantity() + 1);
                    existingItem.setAvailable(currentProduct.isAvailable());
                    cartRef.set(existingItem)
                            .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Quantity updated in cart", Toast.LENGTH_SHORT).show());
                }
            } else {
                String imageUrl = (currentProduct.getImageUrls() != null && !currentProduct.getImageUrls().isEmpty()) 
                        ? currentProduct.getImageUrls().get(0) : "";
                
                CartItem newItem = CartItem.builder()
                        .productId(productId)
                        .productName(currentProduct.getName())
                        .productPrice(currentProduct.getPrice())
                        .quantity(1)
                        .productImage(imageUrl)
                        .available(currentProduct.isAvailable())
                        .build();

                cartRef.set(newItem)
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Added to cart", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}

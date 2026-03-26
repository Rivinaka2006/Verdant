package lk.evolvex.rivinaka.verdant.fragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.BannerAdapter;
import lk.evolvex.rivinaka.verdant.adapter.ForYouProductAdapter;
import lk.evolvex.rivinaka.verdant.adapter.PopularProductAdapter;
import lk.evolvex.rivinaka.verdant.adapter.SpecialOfferAdapter;
import lk.evolvex.rivinaka.verdant.model.Banner;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Product;

public class HomeFragment extends Fragment implements SpecialOfferAdapter.OnProductClickListener, PopularProductAdapter.OnProductClickListener, ForYouProductAdapter.OnProductClickListener {

    private static final String TAG = "HomeFragment";
    private RecyclerView rvSpecialOffers, rvPopularProducts, rvForYou;
    private SpecialOfferAdapter specialOfferAdapter;
    private ForYouProductAdapter forYouAdapter;
    private PopularProductAdapter popularProductAdapter;
    private List<Product> specialOfferList, popularProductList, forYouProductList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private LinearLayout layoutCategories;
    private String selectedCategory = "All";

    private ViewPager2 vpBanners;
    private BannerAdapter bannerAdapter;
    private List<Banner> bannerList;
    private Handler sliderHandler = new Handler();
    
    private Map<String, Integer> soldCountMap = new HashMap<>();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    getFCMToken();
                } else {
                    Toast.makeText(getContext(), "Notifications disabled", Toast.LENGTH_SHORT).show();
                }
            });

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

        vpBanners = view.findViewById(R.id.vpBanners);
        bannerList = new ArrayList<>();
        bannerAdapter = new BannerAdapter(bannerList);
        vpBanners.setAdapter(bannerAdapter);

        rvSpecialOffers = view.findViewById(R.id.rvSpecialOffers);
        specialOfferList = new ArrayList<>();
        specialOfferAdapter = new SpecialOfferAdapter(specialOfferList, this);
        rvSpecialOffers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSpecialOffers.setAdapter(specialOfferAdapter);

        rvPopularProducts = view.findViewById(R.id.rvPopularProducts);
        popularProductList = new ArrayList<>();
        popularProductAdapter = new PopularProductAdapter(popularProductList, this);
        rvPopularProducts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvPopularProducts.setAdapter(popularProductAdapter);

        rvForYou = view.findViewById(R.id.rvForYou);
        forYouProductList = new ArrayList<>();
        forYouAdapter = new ForYouProductAdapter(forYouProductList, this);
        rvForYou.setAdapter(forYouAdapter);

        layoutCategories = view.findViewById(R.id.layoutCategories);
        setupCategoryFilters();

        loadBanners();
        refreshHomeData();
        askNotificationPermission();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                getFCMToken();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            getFCMToken();
        }
    }

    private void getFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    updateTokenInFirestore(token);
                });
    }

    private void updateTokenInFirestore(String token) {
        String userId = mAuth.getUid();
        if (userId != null) {
            db.collection("users").document(userId)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token updated"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to update FCM Token", e));
        }
    }

    private void refreshHomeData() {
        loadSoldCountMap(() -> {
            loadSpecialOffers();
            loadPopularProducts();
            loadForYouProducts();
        });
    }

    private void loadSoldCountMap(Runnable onComplete) {
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
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading sold counts", e);
                    onComplete.run();
                });
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

    private void loadBanners() {
        Log.d(TAG, "Fetching banners from Firestore...");
        db.collection("banners")
                .orderBy("order", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bannerList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Banner banner = document.toObject(Banner.class);
                        if (banner.isActive()) {
                            bannerList.add(banner);
                        }
                    }
                    bannerAdapter.notifyDataSetChanged();
                    if (!bannerList.isEmpty()) {
                        // Start at a very high position in the middle to allow "infinite" sliding
                        int middlePos = Integer.MAX_VALUE / 2;
                        middlePos = middlePos - (middlePos % bannerList.size());
                        vpBanners.setCurrentItem(middlePos, false);
                        startAutoSlider();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore error loading banners: ", e);
                });
    }

    private Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (vpBanners != null && bannerList != null && !bannerList.isEmpty()) {
                vpBanners.setCurrentItem(vpBanners.getCurrentItem() + 1, true);
                sliderHandler.postDelayed(this, 6000); // 6 seconds delay
            }
        }
    };

    private void startAutoSlider() {
        sliderHandler.removeCallbacks(sliderRunnable);
        sliderHandler.postDelayed(sliderRunnable, 6000); // 6 seconds delay
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (bannerList != null && !bannerList.isEmpty()) {
            startAutoSlider();
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
                        // Apply locally calculated sold count
                        product.setSoldCount(soldCountMap.getOrDefault(product.getProductId(), 0));
                        specialOfferList.add(product);
                    }
                    specialOfferAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading special offers", e);
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
                        // Apply locally calculated sold count
                        product.setSoldCount(soldCountMap.getOrDefault(product.getProductId(), 0));
                        popularProductList.add(product);
                    }
                    popularProductAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading popular products", e);
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
                        // Apply locally calculated sold count
                        product.setSoldCount(soldCountMap.getOrDefault(product.getProductId(), 0));
                        forYouProductList.add(product);
                    }
                    forYouAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading For You products", e);
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
            Log.e(TAG, "Error checking cart", e);
        });
    }
}

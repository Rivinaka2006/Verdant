package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.Nursery;

public class ShopDetailsFragment extends Fragment implements OnMapReadyCallback {

    private static final String ARG_SHOP_ID = "shopId";
    private String shopId;
    private FirebaseFirestore db;
    
    private ImageView ivShopLogo;
    private TextView tvShopName, tvShopRating, tvProductCount, tvShopDescription;
    private MapView mapView;
    private GoogleMap googleMap;
    private ProgressBar progressBar;
    private Button btnGetDirections;
    
    private Nursery currentNursery;

    public ShopDetailsFragment() {
        // Required empty public constructor
    }

    public static ShopDetailsFragment newInstance(String shopId) {
        ShopDetailsFragment fragment = new ShopDetailsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SHOP_ID, shopId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            shopId = getArguments().getString(ARG_SHOP_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop_details, container, false);
        
        initViews(view);
        
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        
        if (shopId != null) {
            loadShopDetails();
        } else {
            Toast.makeText(getContext(), "Error: Shop ID missing", Toast.LENGTH_SHORT).show();
        }
        
        return view;
    }

    private void initViews(View view) {
        ivShopLogo = view.findViewById(R.id.iv_shop_logo);
        tvShopName = view.findViewById(R.id.tv_shop_name);
        tvShopRating = view.findViewById(R.id.tv_shop_rating);
        tvProductCount = view.findViewById(R.id.tv_product_count);
        tvShopDescription = view.findViewById(R.id.tv_shop_description);
        mapView = view.findViewById(R.id.map_view);
        progressBar = view.findViewById(R.id.progress_bar);
        btnGetDirections = view.findViewById(R.id.btn_get_directions);
        
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        btnGetDirections.setOnClickListener(v -> {
            if (currentNursery != null) {
                FullScreenMapFragment fragment = FullScreenMapFragment.newInstance(
                        currentNursery.getLatitude(),
                        currentNursery.getLongitude(),
                        currentNursery.getNurseryName()
                );
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            } else {
                Toast.makeText(getContext(), "Loading shop location...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadShopDetails() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("nurseries").document(shopId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentNursery = documentSnapshot.toObject(Nursery.class);
                        if (currentNursery != null) {
                            currentNursery.setNurseryId(documentSnapshot.getId());
                            displayShopData(currentNursery);
                            loadProductCount();
                            updateMapLocation();
                        }
                    } else {
                        // Try finding by ownerId if doc ID isn't shopId
                        db.collection("nurseries").whereEqualTo("ownerId", shopId).get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        currentNursery = queryDocumentSnapshots.getDocuments().get(0).toObject(Nursery.class);
                                        if (currentNursery != null) {
                                            currentNursery.setNurseryId(queryDocumentSnapshots.getDocuments().get(0).getId());
                                            displayShopData(currentNursery);
                                            loadProductCount();
                                            updateMapLocation();
                                        }
                                    } else {
                                        Toast.makeText(getContext(), "Shop details not found", Toast.LENGTH_SHORT).show();
                                    }
                                    progressBar.setVisibility(View.GONE);
                                });
                        return;
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("ShopDetailsFragment", "Error loading shop", e);
                    Toast.makeText(getContext(), "Failed to load shop details", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadProductCount() {
        db.collection("products")
                .whereEqualTo("nurseryId", shopId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    tvProductCount.setText(String.format(Locale.getDefault(), "%d Products", count));
                });
    }

    private void displayShopData(Nursery nursery) {
        tvShopName.setText(nursery.getNurseryName());
        tvShopRating.setText(String.format(Locale.getDefault(), "%.1f (%d reviews)", 
                nursery.getRatingAverage(), nursery.getTotalReviews()));
        tvShopDescription.setText(nursery.getDescription());

        if (nursery.getBannerImageUrl() != null && !nursery.getBannerImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(nursery.getBannerImageUrl())
                    .placeholder(R.drawable.logo)
                    .error(R.drawable.logo)
                    .into(ivShopLogo);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        updateMapLocation();
    }

    private void updateMapLocation() {
        if (googleMap != null && currentNursery != null) {
            double lat = currentNursery.getLatitude();
            double lng = currentNursery.getLongitude();
            
            if (lat != 0 || lng != 0) {
                LatLng shopLocation = new LatLng(lat, lng);
                googleMap.clear();
                googleMap.addMarker(new MarkerOptions()
                        .position(shopLocation)
                        .title(currentNursery.getNurseryName()));
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(shopLocation, 15f));
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}

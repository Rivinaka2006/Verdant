package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.ImageSliderAdapter;
import lk.evolvex.rivinaka.verdant.model.Product;

public class singleProductFragment extends Fragment {

    private String productId;
    private FirebaseFirestore db;
    private TextView tvProductName, tvProductWeight, tvPrice, tvDescription, tvCareInstructions, tvWateringFrequency, tvLightRequirement;
    private ViewPager2 viewPager;

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
        return inflater.inflate(R.layout.fragment_single_product, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        initViews(view);
        setupToolbar(view);

        if (productId != null) {
            loadProductDetails();
        } else {
            Toast.makeText(getContext(), "Product not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews(View view) {
        tvProductName = view.findViewById(R.id.tv_product_name);
        tvProductWeight = view.findViewById(R.id.tv_product_weight);
        tvPrice = view.findViewById(R.id.tv_price);
        tvDescription = view.findViewById(R.id.tv_description_text);
        tvCareInstructions = view.findViewById(R.id.tv_care_instructions_text);
        tvWateringFrequency = view.findViewById(R.id.tv_watering_frequency_text);
        tvLightRequirement = view.findViewById(R.id.tv_light_requirement_text);
        viewPager = view.findViewById(R.id.viewPager_product_media);
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
                        Product product = documentSnapshot.toObject(Product.class);
                        if (product != null) {
                            displayProductData(product);
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

    private void displayProductData(Product product) {
        tvProductName.setText(product.getName());
        tvProductWeight.setText(product.getCategory());
        tvPrice.setText("Rs. " + product.getPrice());
        tvDescription.setText(product.getDescription());
        tvCareInstructions.setText(product.getCareInstructions());
        tvWateringFrequency.setText(product.getWaterFrequency());
        tvLightRequirement.setText(product.getLightRequirement());

        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            ImageSliderAdapter adapter = new ImageSliderAdapter(product.getImageUrls());
            viewPager.setAdapter(adapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            mainHome.setHeaderVisibility(View.VISIBLE);
            mainHome.setBottomNavVisibility(View.VISIBLE);
        }
    }
}

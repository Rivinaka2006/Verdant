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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.SpecialOfferAdapter;
import lk.evolvex.rivinaka.verdant.model.Product;

public class SpecialOffersFragment extends Fragment implements SpecialOfferAdapter.OnProductClickListener {

    private RecyclerView rvSpecialOffers;
    private SpecialOfferAdapter adapter;
    private List<Product> productList;
    private FirebaseFirestore db;
    private Map<String, Integer> soldCountMap = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_special_offers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Hide main header and bottom nav
        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            mainHome.setHeaderVisibility(View.GONE);
            mainHome.setBottomNavVisibility(View.GONE);
        }

        view.findViewById(R.id.btnBack).setOnClickListener(v -> {
            if (getActivity() != null) getActivity().onBackPressed();
        });

        rvSpecialOffers = view.findViewById(R.id.rvSpecialOffersAll);
        productList = new ArrayList<>();
        adapter = new SpecialOfferAdapter(productList, this);

        rvSpecialOffers.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvSpecialOffers.setAdapter(adapter);

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
                    loadAllSpecialOffers();
                })
                .addOnFailureListener(e -> {
                    Log.e("SpecialOffersFragment", "Error loading sold counts", e);
                    loadAllSpecialOffers();
                });
    }

    private void loadAllSpecialOffers() {
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
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("SpecialOffersFragment", "Error loading products", e);
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
}

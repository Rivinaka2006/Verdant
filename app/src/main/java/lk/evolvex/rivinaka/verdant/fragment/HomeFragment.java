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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.SpecialOfferAdapter;
import lk.evolvex.rivinaka.verdant.model.Product;

public class HomeFragment extends Fragment implements SpecialOfferAdapter.OnProductClickListener {

    private RecyclerView rvSpecialOffers;
    private SpecialOfferAdapter specialOfferAdapter;
    private List<Product> specialOfferList;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        // Ensure the top header and bottom navigation are visible
        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            mainHome.setHeaderVisibility(View.VISIBLE);
            mainHome.setBottomNavVisibility(View.VISIBLE);
        }

        rvSpecialOffers = view.findViewById(R.id.rvSpecialOffers);
        specialOfferList = new ArrayList<>();
        specialOfferAdapter = new SpecialOfferAdapter(specialOfferList, this);

        rvSpecialOffers.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        rvSpecialOffers.setAdapter(specialOfferAdapter);

        loadSpecialOffers();
    }

    private void loadSpecialOffers() {
        db.collection("products")
                .whereEqualTo("available", true)
                .limit(10) // You can adjust this or add a specific "isSpecialOffer" field later
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
                    Log.e("HomeFragment", "Error loading products", e);
                    Toast.makeText(getContext(), "Failed to load special offers", Toast.LENGTH_SHORT).show();
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
}

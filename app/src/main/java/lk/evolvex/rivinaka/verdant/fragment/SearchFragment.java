package lk.evolvex.rivinaka.verdant.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.MainHome;
import lk.evolvex.rivinaka.verdant.adapter.ForYouProductAdapter;
import lk.evolvex.rivinaka.verdant.model.Product;

public class SearchFragment extends Fragment implements ForYouProductAdapter.OnProductClickListener {

    private RecyclerView rvSearchResults;
    private ForYouProductAdapter adapter;
    private List<Product> productList;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView tvNoResults, tvSearchTitle;
    private String searchQuery;

    public static SearchFragment newInstance(String query) {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        args.putString("query", query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            searchQuery = getArguments().getString("query");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (getActivity() instanceof MainHome) {
            MainHome mainHome = (MainHome) getActivity();
            mainHome.setHeaderVisibility(View.GONE);
            mainHome.setBottomNavVisibility(View.VISIBLE);
        }

        tvSearchTitle = view.findViewById(R.id.tvSearchTitle);
        rvSearchResults = view.findViewById(R.id.rvSearchResults);
        progressBar = view.findViewById(R.id.progressBar);
        tvNoResults = view.findViewById(R.id.tvNoResults);

        view.findViewById(R.id.btnBack).setOnClickListener(v -> getParentFragmentManager().popBackStack());

        productList = new ArrayList<>();
        adapter = new ForYouProductAdapter(productList, this);
        rvSearchResults.setLayoutManager(new GridLayoutManager(getContext(), 2));
        rvSearchResults.setAdapter(adapter);

        if (searchQuery != null && !searchQuery.isEmpty()) {
            tvSearchTitle.setText("Results for \"" + searchQuery + "\"");
            performSearch(searchQuery);
        }
    }

    private void performSearch(String query) {
        progressBar.setVisibility(View.VISIBLE);
        tvNoResults.setVisibility(View.GONE);

        // Firestore search (case-sensitive and startsWith implementation)
        // Note: For better search, consider using Algolia or ElasticSearch, but for basic search:
        db.collection("products")
                .whereEqualTo("available", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    productList.clear();
                    String lowerQuery = query.toLowerCase();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Product product = document.toObject(Product.class);
                        product.setProductId(document.getId());
                        
                        // Client-side filtering for case-insensitive search
                        if (product.getName().toLowerCase().contains(lowerQuery) || 
                            (product.getDescription() != null && product.getDescription().toLowerCase().contains(lowerQuery))) {
                            productList.add(product);
                        }
                    }
                    
                    progressBar.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                    
                    if (productList.isEmpty()) {
                        tvNoResults.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("SearchFragment", "Error searching products", e);
                    Toast.makeText(getContext(), "Search failed", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onProductClick(Product product) {
        // Navigate to SingleProductFragment
        // Assuming singleProductFragment exists based on HomeFragment code
        try {
            Fragment singleProductFragment = (Fragment) Class.forName("lk.evolvex.rivinaka.verdant.fragment.singleProductFragment").newInstance();
            Bundle bundle = new Bundle();
            bundle.putString("productId", product.getProductId());
            singleProductFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, singleProductFragment)
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

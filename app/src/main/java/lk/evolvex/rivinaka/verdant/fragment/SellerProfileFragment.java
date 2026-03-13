package lk.evolvex.rivinaka.verdant.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.SellerSignIn;
import lk.evolvex.rivinaka.verdant.activity.Welcome;

public class SellerProfileFragment extends Fragment {

    private TextView tvBusinessName, tvLocation, tvContact;
    private MaterialButton btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        tvBusinessName = view.findViewById(R.id.tvBusinessName);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvContact = view.findViewById(R.id.tvContact);
        btnLogout = view.findViewById(R.id.btnLogout);

        loadSellerData();

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), SellerSignIn.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });
    }

    private void loadSellerData() {
        if (mAuth.getCurrentUser() == null) return;

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullName");
                        String phone = documentSnapshot.getString("phone");
                        // Add more fields if they exist in your Firestore user document

                        tvBusinessName.setText(name != null ? name : "N/A");
                        tvContact.setText(phone != null ? phone : "N/A");
                        // Location could be a separate field or part of a nursery profile
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error loading profile", Toast.LENGTH_SHORT).show();
                });
    }
}

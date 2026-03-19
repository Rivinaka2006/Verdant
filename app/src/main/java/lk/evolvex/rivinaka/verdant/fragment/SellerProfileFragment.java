package lk.evolvex.rivinaka.verdant.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.SellerSignIn;

public class SellerProfileFragment extends Fragment {

    private TextView tvBusinessName, tvLocation, tvContact, tvAccountHolder, tvAccountNumber;
    private ImageView ivSellerProfile;
    private MaterialButton btnLogout, btnUpdateBankDetails;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Uri proofImageUri;
    private ImageView ivDialogProofPreview;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    proofImageUri = result.getData().getData();
                    if (ivDialogProofPreview != null && proofImageUri != null) {
                        ivDialogProofPreview.setImageURI(proofImageUri);
                        ivDialogProofPreview.setPadding(0, 0, 0, 0);
                        ivDialogProofPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    }
                }
            }
    );

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
        tvAccountHolder = view.findViewById(R.id.tvAccountHolder);
        tvAccountNumber = view.findViewById(R.id.tvAccountNumber);
        ivSellerProfile = view.findViewById(R.id.ivSellerProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnUpdateBankDetails = view.findViewById(R.id.btnUpdateBankDetails);

        loadSellerData();

        btnUpdateBankDetails.setOnClickListener(v -> showUpdateBankDetailsDialog());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), SellerSignIn.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    private void showUpdateBankDetailsDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_bank_details, null);
        dialog.setContentView(dialogView);

        TextInputEditText etAccountName = dialogView.findViewById(R.id.etAccountName);
        TextInputEditText etAccountNumber = dialogView.findViewById(R.id.etAccountNumber);
        TextInputEditText etBankBranch = dialogView.findViewById(R.id.etBankBranch);
        ivDialogProofPreview = dialogView.findViewById(R.id.ivProofPreview);
        View btnSelectProof = dialogView.findViewById(R.id.btnSelectProof);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmitBankDetails);

        btnSelectProof.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        btnSubmit.setOnClickListener(v -> {
            String name = etAccountName.getText().toString().trim();
            String number = etAccountNumber.getText().toString().trim();
            String bankBranch = etBankBranch.getText().toString().trim();

            if (name.isEmpty() || number.isEmpty() || bankBranch.isEmpty() || proofImageUri == null) {
                Toast.makeText(getContext(), "Please fill all fields and provide proof", Toast.LENGTH_SHORT).show();
                return;
            }

            // Logic to save/upload data would go here
            Toast.makeText(getContext(), "Bank details submitted for verification", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadSellerData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        // First, load basic user data (like address) which is always in the user document
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String address = userDoc.getString("defaultAddressId");
                        if (address != null && !address.isEmpty()) {
                            tvLocation.setText(address);
                        }
                    }
                });

        // Then, try to get nursery data where ownerId matches current user for branding
        db.collection("nurseries").whereEqualTo("ownerId", userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // If nursery document exists, use its data (including bannerImageUrl)
                        com.google.firebase.firestore.DocumentSnapshot nurseryDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String name = nurseryDoc.getString("nurseryName");
                        String phone = nurseryDoc.getString("phoneNumber");
                        String bannerImageUrl = nurseryDoc.getString("bannerImageUrl");
                        
                        // Load bank details if they exist in the nursery doc
                        String accHolder = nurseryDoc.getString("bankAccountName");
                        String accNumber = nurseryDoc.getString("bankAccountNumber");

                        tvBusinessName.setText(name != null ? name : "N/A");
                        tvContact.setText(phone != null ? phone : "N/A");
                        
                        if (accHolder != null) tvAccountHolder.setText(accHolder);
                        if (accNumber != null) tvAccountNumber.setText(accNumber);

                        if (bannerImageUrl != null && !bannerImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(bannerImageUrl)
                                    .placeholder(R.drawable.person_icon)
                                    .error(R.drawable.person_icon)
                                    .circleCrop()
                                    .into(ivSellerProfile);
                        }
                    } else {
                        // Fallback to user document for everything if no nursery document is found
                        loadUserDataFallback(userId);
                    }
                })
                .addOnFailureListener(e -> loadUserDataFallback(userId));
    }

    private void loadUserDataFallback(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("fullName");
                        String phone = documentSnapshot.getString("phone");
                        String profileImage = documentSnapshot.getString("profileImage");
                        String address = documentSnapshot.getString("address");

                        tvBusinessName.setText(name != null ? name : "N/A");
                        tvContact.setText(phone != null ? phone : "N/A");
                        if (address != null && !address.isEmpty()) {
                            tvLocation.setText(address);
                        }

                        if (profileImage != null && !profileImage.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImage)
                                    .placeholder(R.drawable.person_icon)
                                    .error(R.drawable.person_icon)
                                    .circleCrop()
                                    .into(ivSellerProfile);
                        }
                    }
                });
    }
}

package lk.evolvex.rivinaka.verdant.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.SetFingerPrint;
import lk.evolvex.rivinaka.verdant.activity.SignIn;
import lk.evolvex.rivinaka.verdant.model.Address;
import lk.evolvex.rivinaka.verdant.model.User;

public class ProfileFragment extends Fragment {

    private TextInputEditText etFullName, etEmail, etPhone, etAddress;
    private TextInputEditText etBillingFirstName, etBillingLastName, etBillingEmail, etBillingPhone, etBillingAddress, etBillingCity, etBillingPostalCode;
    private TextInputEditText etShippingFirstName, etShippingLastName, etShippingEmail, etShippingPhone, etShippingAddress, etShippingCity, etShippingPostalCode;
    private ImageView ivProfileImage, ivBillingArrow, ivShippingArrow;
    private LinearLayout llBillingHeader, llBillingContent, llShippingHeader, llShippingContent;
    private CheckBox cbSameAsBilling;
    private SwitchMaterial swBiometric;
    private MaterialButton btnSave, btnLogout;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;

    private boolean isBillingExpanded = false;
    private boolean isShippingExpanded = false;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadProfileImage(imageUri);
                    }
                }
            }
    );

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews(view);
        setupExpandableSections();
        setupCheckboxLogic();
        setupBiometricLogic();
        loadUserProfile();

        btnSave.setOnClickListener(v -> saveUserProfile());

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), SignIn.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
        
        ivProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });
    }

    private void initViews(View view) {
        // Profile Image
        ivProfileImage = view.findViewById(R.id.ivProfileImage);

        // General Info Fields
        etFullName = view.findViewById(R.id.etFullName);
        etEmail = view.findViewById(R.id.etEmail);
        etPhone = view.findViewById(R.id.etPhone);
        etAddress = view.findViewById(R.id.etAddress);

        // Biometric Switch
        swBiometric = view.findViewById(R.id.swBiometric);

        // Checkbox
        cbSameAsBilling = view.findViewById(R.id.cbSameAsBilling);

        // Billing Header & Content
        llBillingHeader = view.findViewById(R.id.llBillingHeader);
        llBillingContent = view.findViewById(R.id.llBillingContent);
        ivBillingArrow = view.findViewById(R.id.ivBillingArrow);

        // Billing Fields
        etBillingFirstName = view.findViewById(R.id.etBillingFirstName);
        etBillingLastName = view.findViewById(R.id.etBillingLastName);
        etBillingEmail = view.findViewById(R.id.etBillingEmail);
        etBillingPhone = view.findViewById(R.id.etBillingPhone);
        etBillingAddress = view.findViewById(R.id.etBillingAddress);
        etBillingCity = view.findViewById(R.id.etBillingCity);
        etBillingPostalCode = view.findViewById(R.id.etBillingPostalCode);

        // Shipping Header & Content
        llShippingHeader = view.findViewById(R.id.llShippingHeader);
        llShippingContent = view.findViewById(R.id.llShippingContent);
        ivShippingArrow = view.findViewById(R.id.ivShippingArrow);

        // Shipping Fields
        etShippingFirstName = view.findViewById(R.id.etShippingFirstName);
        etShippingLastName = view.findViewById(R.id.etShippingLastName);
        etShippingEmail = view.findViewById(R.id.etShippingEmail);
        etShippingPhone = view.findViewById(R.id.etShippingPhone);
        etShippingAddress = view.findViewById(R.id.etShippingAddress);
        etShippingCity = view.findViewById(R.id.etShippingCity);
        etShippingPostalCode = view.findViewById(R.id.etShippingPostalCode);

        btnSave = view.findViewById(R.id.btnSave);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void uploadProfileImage(Uri imageUri) {
        if (mAuth.getCurrentUser() == null) return;

        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Uploading profile image...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String fileName = "profile_images/" + mAuth.getCurrentUser().getUid() + "_" + UUID.randomUUID().toString();
        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    db.collection("users").document(mAuth.getCurrentUser().getUid())
                            .update("profileImageUrl", downloadUrl)
                            .addOnSuccessListener(aVoid -> {
                                progressDialog.dismiss();
                                Glide.with(this)
                                        .load(downloadUrl)
                                        .placeholder(R.drawable.person_icon)
                                        .circleCrop()
                                        .into(ivProfileImage);
                                ivProfileImage.setImageTintList(null); // Remove tint once image is loaded
                                Toast.makeText(getContext(), "Profile image updated", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                            });
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void setupExpandableSections() {
        llBillingHeader.setOnClickListener(v -> {
            isBillingExpanded = !isBillingExpanded;
            llBillingContent.setVisibility(isBillingExpanded ? View.VISIBLE : View.GONE);
            ivBillingArrow.setRotation(isBillingExpanded ? 180 : 0);
        });

        llShippingHeader.setOnClickListener(v -> {
            if (cbSameAsBilling.isChecked()) {
                Toast.makeText(getContext(), "Shipping is set same as billing", Toast.LENGTH_SHORT).show();
                return;
            }
            isShippingExpanded = !isShippingExpanded;
            llShippingContent.setVisibility(isShippingExpanded ? View.VISIBLE : View.GONE);
            ivShippingArrow.setRotation(isShippingExpanded ? 180 : 0);
        });
    }

    private void setupCheckboxLogic() {
        cbSameAsBilling.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Collapse and disable shipping section
                isShippingExpanded = false;
                llShippingContent.setVisibility(View.GONE);
                ivShippingArrow.setRotation(0);
                llShippingHeader.setAlpha(0.5f);
            } else {
                llShippingHeader.setAlpha(1.0f);
            }
        });
    }

    private void setupBiometricLogic() {
        swBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                if (isChecked) {
                    Intent intent = new Intent(getContext(), SetFingerPrint.class);
                    startActivity(intent);
                } else {
                    updateBiometricState(false);
                }
            }
        });
    }

    private void updateBiometricState(boolean enabled) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .update("biometricEnabled", enabled)
                .addOnSuccessListener(aVoid -> {
                    String status = enabled ? "enabled" : "disabled";
                    Toast.makeText(getContext(), "Biometric verification " + status, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    swBiometric.setChecked(!enabled); // Revert switch
                    Toast.makeText(getContext(), "Failed to update biometric state", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // General Info
                        etFullName.setText(documentSnapshot.getString("fullName"));
                        etEmail.setText(documentSnapshot.getString("email"));
                        etPhone.setText(documentSnapshot.getString("phone"));
                        etAddress.setText(documentSnapshot.getString("address"));

                        // Profile Image
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.person_icon)
                                    .circleCrop()
                                    .into(ivProfileImage);
                            ivProfileImage.setImageTintList(null); // Crucial: Remove the placeholder tint
                        }

                        // Biometric state
                        Boolean biometricEnabled = documentSnapshot.getBoolean("biometricEnabled");
                        swBiometric.setChecked(biometricEnabled != null && biometricEnabled);

                        // Load Billing Info (Map approach if model is strict)
                        Map<String, Object> billing = (Map<String, Object>) documentSnapshot.get("billing");
                        if (billing != null) {
                            etBillingFirstName.setText((String) billing.get("firstName"));
                            etBillingLastName.setText((String) billing.get("lastName"));
                            etBillingEmail.setText((String) billing.get("email"));
                            etBillingPhone.setText((String) billing.get("phone"));
                            etBillingAddress.setText((String) billing.get("address"));
                            etBillingCity.setText((String) billing.get("city"));
                            etBillingPostalCode.setText((String) billing.get("postalCode"));
                        }

                        // Load Checkbox state
                        Boolean sameAsBilling = documentSnapshot.getBoolean("sameAsBilling");
                        cbSameAsBilling.setChecked(sameAsBilling != null && sameAsBilling);

                        // Load Shipping Info
                        Map<String, Object> shipping = (Map<String, Object>) documentSnapshot.get("shipping");
                        if (shipping != null) {
                            etShippingFirstName.setText((String) shipping.get("firstName"));
                            etShippingLastName.setText((String) shipping.get("lastName"));
                            etShippingEmail.setText((String) shipping.get("email"));
                            etShippingPhone.setText((String) shipping.get("phone"));
                            etShippingAddress.setText((String) shipping.get("address"));
                            etShippingCity.setText((String) shipping.get("city"));
                            etShippingPostalCode.setText((String) shipping.get("postalCode"));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ProfileFragment", "Error loading profile", e));
    }

    private void saveUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String fullName = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String addressStr = etAddress.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Full name is required");
            return;
        }

        Address billing = Address.builder()
                .firstName(etBillingFirstName.getText().toString().trim())
                .lastName(etBillingLastName.getText().toString().trim())
                .email(etBillingEmail.getText().toString().trim())
                .phone(etBillingPhone.getText().toString().trim())
                .address(etBillingAddress.getText().toString().trim())
                .city(etBillingCity.getText().toString().trim())
                .country("Sri Lanka")
                .postalCode(etBillingPostalCode.getText().toString().trim())
                .build();

        Address shipping;
        if (cbSameAsBilling.isChecked()) {
            shipping = billing;
        } else {
            shipping = Address.builder()
                    .firstName(etShippingFirstName.getText().toString().trim())
                    .lastName(etShippingLastName.getText().toString().trim())
                    .email(etShippingEmail.getText().toString().trim())
                    .phone(etShippingPhone.getText().toString().trim())
                    .address(etShippingAddress.getText().toString().trim())
                    .city(etShippingCity.getText().toString().trim())
                    .country("Sri Lanka")
                    .postalCode(etShippingPostalCode.getText().toString().trim())
                    .build();
        }

        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phone", phone);
        updates.put("address", addressStr);
        updates.put("billing", billing);
        updates.put("shipping", shipping);
        updates.put("sameAsBilling", cbSameAsBilling.isChecked());
        updates.put("biometricEnabled", swBiometric.isChecked());

        db.collection("users").document(userId).update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e("ProfileFragment", "Update failed", e);
                    Toast.makeText(getContext(), "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

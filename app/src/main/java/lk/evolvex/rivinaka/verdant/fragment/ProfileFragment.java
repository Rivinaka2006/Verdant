package lk.evolvex.rivinaka.verdant.fragment;

import android.content.Intent;
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
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.SetFingerPrint;
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
    private MaterialButton btnSave;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private boolean isBillingExpanded = false;
    private boolean isShippingExpanded = false;

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

        initViews(view);
        setupExpandableSections();
        setupCheckboxLogic();
        setupBiometricLogic();
        loadUserProfile();

        btnSave.setOnClickListener(v -> saveUserProfile());
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
            if (isChecked && buttonView.isPressed()) {
                Intent intent = new Intent(getContext(), SetFingerPrint.class);
                startActivity(intent);
            }
        });
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            // Load Profile Image
                            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                Glide.with(this)
                                        .load(user.getProfileImageUrl())
                                        .placeholder(R.drawable.person_icon)
                                        .into(ivProfileImage);
                                ivProfileImage.setImageTintList(null);
                            }

                            // General Info
                            etFullName.setText(user.getFullName());
                            etEmail.setText(user.getEmail());
                            etPhone.setText(user.getPhone());
                            etAddress.setText(user.getAddress());

                            // Biometric state
                            swBiometric.setChecked(user.getBiometricEnabled() != null && user.getBiometricEnabled());

                            // Load Billing Info
                            Address billing = user.getBilling();
                            if (billing != null) {
                                etBillingFirstName.setText(billing.getFirstName());
                                etBillingLastName.setText(billing.getLastName());
                                etBillingEmail.setText(billing.getEmail());
                                etBillingPhone.setText(billing.getPhone());
                                etBillingAddress.setText(billing.getAddress());
                                etBillingCity.setText(billing.getCity());
                                etBillingPostalCode.setText(billing.getPostalCode());
                            }

                            // Load Checkbox state
                            cbSameAsBilling.setChecked(user.getSameAsBilling() != null && user.getSameAsBilling());

                            // Load Shipping Info
                            Address shipping = user.getShipping();
                            if (shipping != null) {
                                etShippingFirstName.setText(shipping.getFirstName());
                                etShippingLastName.setText(shipping.getLastName());
                                etShippingEmail.setText(shipping.getEmail());
                                etShippingPhone.setText(shipping.getPhone());
                                etShippingAddress.setText(shipping.getAddress());
                                etShippingCity.setText(shipping.getCity());
                                etShippingPostalCode.setText(shipping.getPostalCode());
                            }
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

package lk.evolvex.rivinaka.verdant.fragment;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.SellerSignIn;

public class SellerProfileFragment extends Fragment {

    private TextView tvBusinessName, tvLocation, tvContact, tvAccountHolder, tvAccountNumber, tvCoordinates;
    private ImageView ivSellerProfile;
    private MaterialButton btnLogout, btnUpdateBankDetails, btnUpdateLocation;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FusedLocationProviderClient fusedLocationClient;

    private Uri proofImageUri;
    private ImageView ivDialogProofPreview;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    getCurrentLocation();
                } else if (coarseLocationGranted != null && coarseLocationGranted) {
                    getCurrentLocation();
                } else {
                    Toast.makeText(getContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            }
    );

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
        storage = FirebaseStorage.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        tvBusinessName = view.findViewById(R.id.tvBusinessName);
        tvLocation = view.findViewById(R.id.tvLocation);
        tvCoordinates = view.findViewById(R.id.tvCoordinates);
        tvContact = view.findViewById(R.id.tvContact);
        tvAccountHolder = view.findViewById(R.id.tvAccountHolder);
        tvAccountNumber = view.findViewById(R.id.tvAccountNumber);
        ivSellerProfile = view.findViewById(R.id.ivSellerProfile);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnUpdateBankDetails = view.findViewById(R.id.btnUpdateBankDetails);
        btnUpdateLocation = view.findViewById(R.id.btnUpdateLocation);

        loadSellerData();

        btnUpdateBankDetails.setOnClickListener(v -> showUpdateBankDetailsDialog());
        btnUpdateLocation.setOnClickListener(v -> checkLocationPermission());

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

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Getting current location...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        updateNurseryLocation(location.getLatitude(), location.getLongitude(), progressDialog);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Could not get location. Make sure GPS is on.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateNurseryLocation(double latitude, double longitude, ProgressDialog progressDialog) {
        if (mAuth.getCurrentUser() == null) {
            progressDialog.dismiss();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);

        db.collection("nurseries").whereEqualTo("ownerId", userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("nurseries").document(docId)
                                .update(locationData)
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Location updated successfully", Toast.LENGTH_SHORT).show();
                                    tvCoordinates.setText(String.format("Lat: %.6f, Lon: %.6f", latitude, longitude));
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Failed to update location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Nursery profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        proofImageUri = null; // Reset for new entry

        btnSelectProof.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        });

        btnSubmit.setOnClickListener(v -> {
            String name = etAccountName.getText().toString().trim();
            String number = etAccountNumber.getText().toString().trim();
            String bankBranch = etBankBranch.getText().toString().trim();

            if (name.isEmpty() || number.isEmpty() || bankBranch.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all bank details", Toast.LENGTH_SHORT).show();
                return;
            }

            if (proofImageUri == null) {
                Toast.makeText(getContext(), "Please upload bank slip/e-statement proof", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadProofAndSubmit(name, number, bankBranch, dialog);
        });

        dialog.show();
    }

    private void uploadProofAndSubmit(String name, String number, String bankBranch, BottomSheetDialog dialog) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Uploading bank details...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String fileName = "bank_proofs/" + UUID.randomUUID().toString();
        StorageReference ref = storage.getReference().child(fileName);

        ref.putFile(proofImageUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateBankDetailsInFirestore(name, number, bankBranch, uri.toString(), dialog, progressDialog);
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateBankDetailsInFirestore(String name, String number, String bankBranch, String proofUrl, BottomSheetDialog dialog, ProgressDialog progressDialog) {
        if (mAuth.getCurrentUser() == null) {
            progressDialog.dismiss();
            return;
        }
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> bankData = new HashMap<>();
        bankData.put("bankAccountName", name);
        bankData.put("bankAccountNumber", number);
        bankData.put("bankNameBranch", bankBranch);
        bankData.put("bankProofUrl", proofUrl);
        bankData.put("bankVerified", false);

        db.collection("nurseries").whereEqualTo("ownerId", userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("nurseries").document(docId)
                                .set(bankData, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Bank details submitted for verification", Toast.LENGTH_SHORT).show();
                                    tvAccountHolder.setText(name);
                                    tvAccountNumber.setText(number);
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "Nursery profile not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSellerData() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        String address = userDoc.getString("defaultAddressId");
                        if (address != null && !address.isEmpty()) {
                            tvLocation.setText(address);
                        }
                    }
                });

        db.collection("nurseries").whereEqualTo("ownerId", userId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot nurseryDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String name = nurseryDoc.getString("nurseryName");
                        String phone = nurseryDoc.getString("phoneNumber");
                        String bannerImageUrl = nurseryDoc.getString("bannerImageUrl");
                        
                        Double lat = nurseryDoc.getDouble("latitude");
                        Double lon = nurseryDoc.getDouble("longitude");
                        
                        String accHolder = nurseryDoc.getString("bankAccountName");
                        String accNumber = nurseryDoc.getString("bankAccountNumber");

                        tvBusinessName.setText(name != null ? name : "N/A");
                        tvContact.setText(phone != null ? phone : "N/A");
                        
                        if (lat != null && lon != null) {
                            tvCoordinates.setText(String.format("Lat: %.6f, Lon: %.6f", lat, lon));
                        }

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

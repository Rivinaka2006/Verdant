package lk.evolvex.rivinaka.verdant.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.Product;

public class AddEditProductActivity extends AppCompatActivity {

    private TextInputEditText etPlantName, etPrice, etStock, etDescription, etCareInstructions, etWaterFrequency;
    private AutoCompleteTextView spinnerCategory, spinnerLight;
    private MaterialButton btnSave, btnUploadImages, btnUploadVideo;
    private TextView tvMediaStatus;
    private ProgressBar progressBar;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    
    private String productId;
    private boolean isEditMode = false;
    private Product existingProduct;
    
    private List<Uri> imageUris = new ArrayList<>();
    private Uri videoUri;
    private List<String> uploadedImageUrls = new ArrayList<>();
    private String uploadedVideoUrl;

    private final ActivityResultLauncher<Intent> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            imageUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        imageUris.add(result.getData().getData());
                    }
                    updateMediaStatus();
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    videoUri = result.getData().getData();
                    updateMediaStatus();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_product);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance("gs://verdant-bbf83.firebasestorage.app");

        initViews();
        setupDropdowns();

        productId = getIntent().getStringExtra("productId");
        if (productId != null) {
            isEditMode = true;
            loadProductDetails();
            btnSave.setText("Update Product");
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnUploadImages.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickImagesLauncher.launch(intent);
        });

        btnUploadVideo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            pickVideoLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> startUploadProcess());
    }

    private void initViews() {
        etPlantName = findViewById(R.id.etPlantName);
        etPrice = findViewById(R.id.etPrice);
        etStock = findViewById(R.id.etStock);
        etDescription = findViewById(R.id.etDescription);
        etCareInstructions = findViewById(R.id.etCareInstructions);
        etWaterFrequency = findViewById(R.id.etWaterFrequency);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerLight = findViewById(R.id.spinnerLight);
        btnSave = findViewById(R.id.btnSave);
        btnUploadImages = findViewById(R.id.btnUploadImages);
        btnUploadVideo = findViewById(R.id.btnUploadVideo);
        
        // Ensure these exist in XML or handle nulls
        tvMediaStatus = findViewById(R.id.tvMediaStatus);
        progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) progressBar.setVisibility(View.GONE);
    }

    private void updateMediaStatus() {
        if (tvMediaStatus == null) return;
        String status = imageUris.size() + " images selected";
        if (videoUri != null) status += ", 1 video selected";
        tvMediaStatus.setText(status);
        tvMediaStatus.setVisibility(View.VISIBLE);
    }

    private void setupDropdowns() {
        String[] categories = {"Indoor Plants", "Outdoor Plants", "Succulents", "Flowering Plants", "Herbs"};
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        spinnerCategory.setAdapter(catAdapter);

        String[] lightReqs = {"Low Light", "Indirect Sunlight", "Direct Sunlight", "Partial Shade"};
        ArrayAdapter<String> lightAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, lightReqs);
        spinnerLight.setAdapter(lightAdapter);
    }

    private void loadProductDetails() {
        db.collection("products").document(productId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        existingProduct = documentSnapshot.toObject(Product.class);
                        if (existingProduct != null) {
                            etPlantName.setText(existingProduct.getName());
                            etPrice.setText(String.valueOf(existingProduct.getPrice()));
                            etStock.setText(String.valueOf(existingProduct.getStock()));
                            etDescription.setText(existingProduct.getDescription());
                            etCareInstructions.setText(existingProduct.getCareInstructions());
                            etWaterFrequency.setText(existingProduct.getWaterFrequency());
                            spinnerCategory.setText(existingProduct.getCategory(), false);
                            spinnerLight.setText(existingProduct.getLightRequirement(), false);
                            
                            uploadedImageUrls = existingProduct.getImageUrls();
                            uploadedVideoUrl = existingProduct.getVideoUrl();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load product", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void startUploadProcess() {
        String name = etPlantName.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mAuth.getCurrentUser() == null) return;

        btnSave.setEnabled(false);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        if (!imageUris.isEmpty()) {
            uploadImages(0);
        } else if (videoUri != null) {
            uploadVideo();
        } else {
            saveToFirestore();
        }
    }

    private void uploadImages(int index) {
        if (index >= imageUris.size()) {
            if (videoUri != null) {
                uploadVideo();
            } else {
                saveToFirestore();
            }
            return;
        }

        StorageReference ref = storage.getReference().child("products/images/" + UUID.randomUUID().toString());
        ref.putFile(imageUris.get(index))
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    uploadedImageUrls.add(uri.toString());
                    uploadImages(index + 1);
                }))
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void uploadVideo() {
        StorageReference ref = storage.getReference().child("products/videos/" + UUID.randomUUID().toString());
        ref.putFile(videoUri)
                .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    uploadedVideoUrl = uri.toString();
                    saveToFirestore();
                }))
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Video upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToFirestore() {
        String name = etPlantName.getText().toString().trim();
        double price = Double.parseDouble(etPrice.getText().toString().trim());
        int stock = Integer.parseInt(etStock.getText().toString().trim());
        String category = spinnerCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String careInstructions = etCareInstructions.getText().toString().trim();
        String waterFreq = etWaterFrequency.getText().toString().trim();
        String lightReq = spinnerLight.getText().toString().trim();
        String sellerId = mAuth.getCurrentUser().getUid();

        if (isEditMode) {
            updateProduct(name, price, stock, category, description, careInstructions, waterFreq, lightReq);
        } else {
            addNewProduct(name, price, stock, category, description, careInstructions, waterFreq, lightReq, sellerId);
        }
    }

    private void addNewProduct(String name, double price, int stock, String category, String description,
                               String careInstructions, String waterFreq, String lightReq, String sellerId) {
        Product product = Product.builder()
                .name(name)
                .price(price)
                .oldPrice(price * 1.2)
                .stock(stock)
                .category(category)
                .description(description)
                .careInstructions(careInstructions)
                .waterFrequency(waterFreq)
                .lightRequirement(lightReq)
                .nurseryId(sellerId)
                .available(stock > 0)
                .imageUrls(uploadedImageUrls)
                .videoUrl(uploadedVideoUrl)
                .createdAt(Timestamp.now())
                .rating(0.0)
                .soldCount(0)
                .build();

        db.collection("products").add(product)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProduct(String name, double price, int stock, String category, String description,
                               String careInstructions, String waterFreq, String lightReq) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("price", price);
        updates.put("stock", stock);
        updates.put("category", category);
        updates.put("description", description);
        updates.put("careInstructions", careInstructions);
        updates.put("waterFrequency", waterFreq);
        updates.put("lightRequirement", lightReq);
        updates.put("available", stock > 0);
        updates.put("imageUrls", uploadedImageUrls);
        updates.put("videoUrl", uploadedVideoUrl);

        db.collection("products").document(productId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

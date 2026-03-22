package lk.evolvex.rivinaka.verdant.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
    private TextView tvMediaStatus, tvProgressPercent;
    private ProgressBar progressBar;
    
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    
    private String productId;
    private boolean isEditMode = false;
    private Product existingProduct;
    
    private List<MediaItem> mediaItems = new ArrayList<>();
    private MediaAdapter mediaAdapter;
    private RecyclerView rvMediaPreview;

    private static class MediaItem {
        Uri uri;
        String url;
        boolean isVideo;

        MediaItem(Uri uri, boolean isVideo) {
            this.uri = uri;
            this.isVideo = isVideo;
        }

        MediaItem(String url, boolean isVideo) {
            this.url = url;
            this.isVideo = isVideo;
        }
    }

    private final ActivityResultLauncher<Intent> pickImagesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    List<Uri> selectedUris = new ArrayList<>();
                    if (result.getData().getClipData() != null) {
                        int count = result.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            selectedUris.add(result.getData().getClipData().getItemAt(i).getUri());
                        }
                    } else if (result.getData().getData() != null) {
                        selectedUris.add(result.getData().getData());
                    }

                    int currentImages = 0;
                    for (MediaItem item : mediaItems) if (!item.isVideo) currentImages++;
                    
                    int canAdd = 3 - currentImages;

                    if (canAdd <= 0) {
                        Toast.makeText(this, "Limit reached: 3 images maximum", Toast.LENGTH_SHORT).show();
                    } else {
                        for (int i = 0; i < Math.min(selectedUris.size(), canAdd); i++) {
                            mediaItems.add(new MediaItem(selectedUris.get(i), false));
                        }
                        if (selectedUris.size() > canAdd) {
                            Toast.makeText(this, "Only " + canAdd + " more images added (Limit: 3)", Toast.LENGTH_SHORT).show();
                        }
                    }
                    mediaAdapter.notifyDataSetChanged();
                    updateMediaStatus();
                }
            }
    );

    private final ActivityResultLauncher<Intent> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    boolean hasVideo = false;
                    for (MediaItem item : mediaItems) {
                        if (item.isVideo) {
                            hasVideo = true;
                            break;
                        }
                    }

                    if (hasVideo) {
                        Toast.makeText(this, "Only 1 video allowed. Delete existing one first.", Toast.LENGTH_SHORT).show();
                    } else {
                        mediaItems.add(new MediaItem(result.getData().getData(), true));
                        mediaAdapter.notifyDataSetChanged();
                        updateMediaStatus();
                    }
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
            int currentImages = 0;
            for (MediaItem item : mediaItems) if (!item.isVideo) currentImages++;
            if (currentImages >= 3) {
                Toast.makeText(this, "Already reached 3 images limit.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            pickImagesLauncher.launch(intent);
        });

        btnUploadVideo.setOnClickListener(v -> {
            boolean hasVideo = false;
            for (MediaItem item : mediaItems) if (item.isVideo) hasVideo = true;
            if (hasVideo) {
                Toast.makeText(this, "Already reached video limit (1).", Toast.LENGTH_SHORT).show();
                return;
            }
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
        tvMediaStatus = findViewById(R.id.tvMediaStatus);
        tvProgressPercent = findViewById(R.id.tvProgressPercent);
        progressBar = findViewById(R.id.progressBar);
        
        rvMediaPreview = new RecyclerView(this);
        rvMediaPreview.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        rvMediaPreview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        mediaAdapter = new MediaAdapter(mediaItems, position -> {
            mediaItems.remove(position);
            mediaAdapter.notifyDataSetChanged();
            updateMediaStatus();
        });
        rvMediaPreview.setAdapter(mediaAdapter);

        // Replace layoutImagePreview with rvMediaPreview in the UI
        View previewContainer = findViewById(R.id.layoutImagePreview);
        ViewGroup parent = (ViewGroup) previewContainer.getParent();
        int index = parent.indexOfChild(previewContainer);
        parent.removeView(previewContainer);
        parent.addView(rvMediaPreview, index);

        // Hide old video preview frame if it exists
        View videoFrame = findViewById(R.id.frameVideoPreview);
        if (videoFrame != null) videoFrame.setVisibility(View.GONE);
        
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
            progressBar.setProgress(0);
        }
        if (tvProgressPercent != null) tvProgressPercent.setVisibility(View.GONE);
    }

    private void updateMediaStatus() {
        if (tvMediaStatus == null) return;
        int images = 0;
        int videos = 0;
        for (MediaItem item : mediaItems) {
            if (item.isVideo) videos++;
            else images++;
        }
        String status = images + "/3 images, " + videos + "/1 video selected";
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
                            
                            mediaItems.clear();
                            if (existingProduct.getImageUrls() != null) {
                                for (String url : existingProduct.getImageUrls()) {
                                    mediaItems.add(new MediaItem(url, false));
                                }
                            }
                            if (existingProduct.getVideoUrl() != null && !existingProduct.getVideoUrl().isEmpty()) {
                                mediaItems.add(new MediaItem(existingProduct.getVideoUrl(), true));
                            }
                            mediaAdapter.notifyDataSetChanged();
                            updateMediaStatus();
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
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
        }
        if (tvProgressPercent != null) {
            tvProgressPercent.setVisibility(View.VISIBLE);
            tvProgressPercent.setText("0%");
        }

        uploadMedia(0, new ArrayList<>(), null);
    }

    private void uploadMedia(int index, List<String> imageUrls, String videoUrl) {
        if (index >= mediaItems.size()) {
            saveToFirestore(imageUrls, videoUrl);
            return;
        }

        MediaItem item = mediaItems.get(index);
        if (item.url != null) {
            // Already uploaded
            if (item.isVideo) {
                uploadMedia(index + 1, imageUrls, item.url);
            } else {
                imageUrls.add(item.url);
                uploadMedia(index + 1, imageUrls, videoUrl);
            }
            return;
        }

        // Need to upload
        String path = item.isVideo ? "products/videos/" : "products/images/";
        StorageReference ref = storage.getReference().child(path + UUID.randomUUID().toString());
        UploadTask uploadTask = ref.putFile(item.uri);
        
        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            int currentProgress = (int) (((index * 100.0) + progress) / mediaItems.size());
            updateProgress(currentProgress);
        });

        uploadTask.addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
                    if (item.isVideo) {
                        uploadMedia(index + 1, imageUrls, uri.toString());
                    } else {
                        imageUrls.add(uri.toString());
                        uploadMedia(index + 1, imageUrls, videoUrl);
                    }
                }))
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    hideProgress();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProgress(int progress) {
        if (progressBar != null) progressBar.setProgress(progress);
        if (tvProgressPercent != null) tvProgressPercent.setText(progress + "%");
    }

    private void hideProgress() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (tvProgressPercent != null) tvProgressPercent.setVisibility(View.GONE);
    }

    private void saveToFirestore(List<String> imageUrls, String videoUrl) {
        updateProgress(100);
        String name = etPlantName.getText().toString().trim();
        double price = Double.parseDouble(etPrice.getText().toString().trim());
        int stock = Integer.parseInt(etStock.getText().toString().trim());
        String category = spinnerCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String careInstructions = etCareInstructions.getText().toString().trim();
        String waterFreq = etWaterFrequency.getText().toString().trim();
        String lightReq = spinnerLight.getText().toString().trim();
        String sellerId = mAuth.getCurrentUser().getUid();

        Map<String, Object> productData = new HashMap<>();
        productData.put("name", name);
        productData.put("price", price);
        productData.put("stock", stock);
        productData.put("category", category);
        productData.put("description", description);
        productData.put("careInstructions", careInstructions);
        productData.put("waterFrequency", waterFreq);
        productData.put("lightRequirement", lightReq);
        productData.put("available", stock > 0);
        productData.put("imageUrls", imageUrls);
        productData.put("videoUrl", videoUrl);

        if (isEditMode) {
            db.collection("products").document(productId).update(productData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        hideProgress();
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            productData.put("nurseryId", sellerId);
            productData.put("oldPrice", price * 1.2);
            productData.put("createdAt", Timestamp.now());
            productData.put("rating", 0.0);
            productData.put("soldCount", 0);
            
            db.collection("products").add(productData)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Product added successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        hideProgress();
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Media Adapter
    private static class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {
        private final List<MediaItem> items;
        private final OnDeleteClickListener listener;

        interface OnDeleteClickListener {
            void onDelete(int position);
        }

        MediaAdapter(List<MediaItem> items, OnDeleteClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_media_preview, parent, false);
            return new MediaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
            MediaItem item = items.get(position);
            holder.ivPlayIcon.setVisibility(item.isVideo ? View.VISIBLE : View.GONE);
            
            if (item.url != null) {
                Glide.with(holder.itemView.getContext()).load(item.url).into(holder.ivPreview);
            } else {
                holder.ivPreview.setImageURI(item.uri);
            }

            holder.btnDelete.setOnClickListener(v -> listener.onDelete(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class MediaViewHolder extends RecyclerView.ViewHolder {
            ImageView ivPreview, ivPlayIcon;
            View btnDelete;

            MediaViewHolder(@NonNull View itemView) {
                super(itemView);
                ivPreview = itemView.findViewById(R.id.ivPreview);
                ivPlayIcon = itemView.findViewById(R.id.ivPlayIcon);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}

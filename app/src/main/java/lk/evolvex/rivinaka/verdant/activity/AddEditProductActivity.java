package lk.evolvex.rivinaka.verdant.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.Product;

public class AddEditProductActivity extends AppCompatActivity {

    private TextInputEditText etPlantName, etPrice, etStock, etDescription, etCareInstructions, etWaterFrequency;
    private AutoCompleteTextView spinnerCategory, spinnerLight;
    private MaterialButton btnSave;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String productId;
    private boolean isEditMode = false;
    private Product existingProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_edit_product);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

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

        btnSave.setOnClickListener(v -> saveProduct());
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
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load product", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void saveProduct() {
        String name = etPlantName.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String stockStr = etStock.getText().toString().trim();
        String category = spinnerCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String careInstructions = etCareInstructions.getText().toString().trim();
        String waterFreq = etWaterFrequency.getText().toString().trim();
        String lightReq = spinnerLight.getText().toString().trim();

        if (name.isEmpty() || priceStr.isEmpty() || stockStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);
        int stock = Integer.parseInt(stockStr);

        if (mAuth.getCurrentUser() == null) return;
        String sellerId = mAuth.getCurrentUser().getUid();

        btnSave.setEnabled(false);

        if (isEditMode) {
            updateProduct(name, price, stock, category, description, careInstructions, waterFreq, lightReq);
        } else {
            addNewProduct(name, price, stock, category, description, careInstructions, waterFreq, lightReq, sellerId);
        }
    }

    private void addNewProduct(String name, double price, int stock, String category, String description,
                               String careInstructions, String waterFreq, String lightReq, String sellerId) {
        List<String> dummyImageUrls = new ArrayList<>();
        dummyImageUrls.add("https://images.unsplash.com/photo-1597055181300-e3633a207519");

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
                .imageUrls(dummyImageUrls)
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

        db.collection("products").document(productId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Product updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}

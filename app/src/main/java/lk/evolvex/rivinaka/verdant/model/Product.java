package lk.evolvex.rivinaka.verdant.model;

import com.google.firebase.Timestamp;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    private String productId;
    private String name;
    private String description;
    private String category;
    private double price;
    private double oldPrice;
    private int stock;
    private boolean available;
    private String nurseryId;
    private List<String> imageUrls;
    private String videoUrl;
    private String lightRequirement;
    private String waterFrequency;
    private String careInstructions;
    private double rating;
    private int ratingCount;
    private int soldCount;
    private Timestamp createdAt;

}

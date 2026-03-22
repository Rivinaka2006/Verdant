package lk.evolvex.rivinaka.verdant.model;

import com.google.firebase.Timestamp;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Nursery {

    private String nurseryId;
    private String nurseryName;
    private String ownerId;
    private String description;
    private String bannerImageUrl;
    private String phoneNumber;
    private double latitude;
    private double longitude;
    private Map<String, String> businessHours;
    private double ratingAverage;
    private int totalReviews;
    private Timestamp time;
    private Timestamp createdAt;

    // Bank Details
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankNameBranch;
    private String bankProofUrl;
    private boolean isBankVerified;

}

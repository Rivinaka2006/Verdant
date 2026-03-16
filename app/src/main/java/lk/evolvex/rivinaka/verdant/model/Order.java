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
public class Order {
    private String orderId;
    private String userId;
    private List<CartItem> items;
    private double totalAmount;
    private double shippingFee;
    private String address;
    private String paymentMethod;
    private String status;
    private Timestamp createdAt;
}

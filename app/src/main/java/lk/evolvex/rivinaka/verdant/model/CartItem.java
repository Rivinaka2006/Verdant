package lk.evolvex.rivinaka.verdant.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem implements Serializable {

    private String productId;
    private String productName;
    private double productPrice;
    private int quantity;
    private String productImage;
    private boolean available;

}

package lk.evolvex.rivinaka.verdant.model;

public class CartItem {

    private String productName;
    private String productPrice;
    private int quantity;
    private int productImage;

    public CartItem(String productName, String productPrice, int quantity, int productImage) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.quantity = quantity;
        this.productImage = productImage;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductPrice() {
        return productPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getProductImage() {
        return productImage;
    }
}

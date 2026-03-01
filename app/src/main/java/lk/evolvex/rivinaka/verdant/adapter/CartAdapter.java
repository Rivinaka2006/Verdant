package lk.evolvex.rivinaka.verdant.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.CartItem;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;

    public CartAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.tvProductName.setText(item.getProductName());
        holder.tvProductPrice.setText(item.getProductPrice());
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        // You can also load the product image here using a library like Glide or Picasso
        // holder.ivProductImage.setImageResource(item.getProductImage());
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {

        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvProductPrice;
        TextView tvQuantity;
        ImageButton btnDecrement;
        ImageButton btnIncrement;
        ImageView ivDelete;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnDecrement = itemView.findViewById(R.id.btnDecrement);
            btnIncrement = itemView.findViewById(R.id.btnIncrement);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}

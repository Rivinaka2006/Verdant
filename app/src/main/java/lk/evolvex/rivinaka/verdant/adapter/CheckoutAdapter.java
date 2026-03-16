package lk.evolvex.rivinaka.verdant.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.CartItem;

public class CheckoutAdapter extends RecyclerView.Adapter<CheckoutAdapter.CheckoutViewHolder> {

    private List<CartItem> cartItems;

    public CheckoutAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CheckoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reusing item_popular_product or similar or a custom small layout
        // For now, let's use a simple layout for checkout items
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item, parent, false);
        return new CheckoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CheckoutViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.tvProductName.setText(item.getProductName());
        holder.tvProductPrice.setText(String.format("Rs. %.2f", item.getProductPrice()));
        holder.tvQuantity.setText("x" + item.getQuantity());
        
        // Hide edit controls for checkout
        if (holder.btnIncrement != null) holder.btnIncrement.setVisibility(View.GONE);
        if (holder.btnDecrement != null) holder.btnDecrement.setVisibility(View.GONE);
        if (holder.ivDelete != null) holder.ivDelete.setVisibility(View.GONE);

        Glide.with(holder.itemView.getContext())
                .load(item.getProductImage())
                .placeholder(R.drawable.sample_plant)
                .into(holder.ivProductImage);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CheckoutViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName, tvProductPrice, tvQuantity;
        View btnIncrement, btnDecrement, ivDelete;

        public CheckoutViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            btnIncrement = itemView.findViewById(R.id.btnIncrement);
            btnDecrement = itemView.findViewById(R.id.btnDecrement);
            ivDelete = itemView.findViewById(R.id.ivDelete);
        }
    }
}

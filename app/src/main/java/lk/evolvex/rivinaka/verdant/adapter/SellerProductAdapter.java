package lk.evolvex.rivinaka.verdant.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.Product;

public class SellerProductAdapter extends RecyclerView.Adapter<SellerProductAdapter.SellerProductViewHolder> {

    private List<Product> productList;
    private OnProductActionListener listener;

    public interface OnProductActionListener {
        void onEditClick(Product product);
        void onToggleAvailability(Product product, boolean isAvailable);
    }

    public SellerProductAdapter(List<Product> productList, OnProductActionListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SellerProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seller_product, parent, false);
        return new SellerProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SellerProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class SellerProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvProductName, tvStock, tvPrice;
        ImageButton btnEdit;
        SwitchCompat switchAvailability;

        public SellerProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvStock = itemView.findViewById(R.id.tvStock);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            switchAvailability = itemView.findViewById(R.id.switchAvailability);
        }

        public void bind(Product product, OnProductActionListener listener) {
            tvProductName.setText(product.getName());
            tvStock.setText("Stock: " + product.getStock());
            tvPrice.setText("Rs. " + product.getPrice());
            switchAvailability.setChecked(product.isAvailable());

            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(product.getImageUrls().get(0))
                        .placeholder(R.drawable.sample_plant)
                        .into(ivProduct);
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(product);
            });

            switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) listener.onToggleAvailability(product, isChecked);
            });
        }
    }
}

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
import lk.evolvex.rivinaka.verdant.model.Product;

public class PopularProductAdapter extends RecyclerView.Adapter<PopularProductAdapter.PopularProductViewHolder> {

    private List<Product> productList;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
        void onAddToCartClick(Product product);
    }

    public PopularProductAdapter(List<Product> productList, OnProductClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PopularProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_popular_product, parent, false);
        return new PopularProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PopularProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class PopularProductViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvProductName, tvRatingAndSold, tvPrice;
        ImageView btnAdd;

        public PopularProductViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProduct);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvRatingAndSold = itemView.findViewById(R.id.tvRatingAndSold);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnAdd = itemView.findViewById(R.id.btnAdd);
        }

        public void bind(Product product, OnProductClickListener listener) {
            tvProductName.setText(product.getName());
            tvPrice.setText("Rs. " + product.getPrice());
            tvRatingAndSold.setText(product.getRating() + " (" + product.getSoldCount() + " sold)");

            if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(product.getImageUrls().get(0))
                        .placeholder(R.drawable.sample_plant)
                        .into(ivProduct);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
            });

            btnAdd.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToCartClick(product);
                }
            });
        }
    }
}

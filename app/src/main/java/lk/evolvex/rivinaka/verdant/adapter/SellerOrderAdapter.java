package lk.evolvex.rivinaka.verdant.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Order;

public class SellerOrderAdapter extends RecyclerView.Adapter<SellerOrderAdapter.OrderViewHolder> {

    private List<Order> orders;
    private OnOrderActionClickListener listener;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, yyyy HH:mm", Locale.getDefault());

    public interface OnOrderActionClickListener {
        void onActionClick(Order order, String action);
    }

    public SellerOrderAdapter(List<Order> orders, OnOrderActionClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seller_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.tvOrderId.setText("#" + (order.getOrderId().length() > 8 ? order.getOrderId().substring(0, 8).toUpperCase() : order.getOrderId().toUpperCase()));
        if (order.getCreatedAt() != null) {
            holder.tvOrderDate.setText(sdf.format(order.getCreatedAt().toDate()));
        }

        // Clear existing items in container to avoid duplication during recycling
        holder.llOrderItemsContainer.removeAllViews();

        // Inflate and add each product in the order array
        if (order.getItems() != null) {
            LayoutInflater inflater = LayoutInflater.from(holder.itemView.getContext());
            for (CartItem item : order.getItems()) {
                View itemView = inflater.inflate(R.layout.item_order_sub_item, holder.llOrderItemsContainer, false);
                
                ImageView ivProduct = itemView.findViewById(R.id.ivProduct);
                TextView tvProductName = itemView.findViewById(R.id.tvProductName);
                TextView tvQuantity = itemView.findViewById(R.id.tvQuantity);
                TextView tvItemPrice = itemView.findViewById(R.id.tvPrice);

                tvProductName.setText(item.getProductName());
                tvQuantity.setText("Qty = " + item.getQuantity());
                tvItemPrice.setText(String.format("Rs. %.2f", item.getProductPrice()));

                Glide.with(holder.itemView.getContext())
                        .load(item.getProductImage())
                        .placeholder(R.drawable.plant)
                        .into(ivProduct);

                holder.llOrderItemsContainer.addView(itemView);
            }
        }

        holder.tvCustomerName.setText("Customer ID: " + (order.getUserId().length() > 6 ? order.getUserId().substring(0, 6) : order.getUserId()));
        holder.tvOrderTotal.setText(String.format("Rs. %.2f", order.getTotalAmount()));

        // Adjust buttons based on status
        setupButtons(holder, order.getStatus());

        holder.btnPrimary.setOnClickListener(v -> {
            if (listener != null) listener.onActionClick(order, holder.btnPrimary.getText().toString());
        });

        holder.btnSecondary.setOnClickListener(v -> {
            if (listener != null) listener.onActionClick(order, holder.btnSecondary.getText().toString());
        });
    }

    private void setupButtons(OrderViewHolder holder, String status) {
        holder.btnSecondary.setVisibility(View.VISIBLE);
        holder.btnPrimary.setVisibility(View.VISIBLE);

        switch (status) {
            case "Pending":
                holder.btnSecondary.setText("Cancel");
                holder.btnPrimary.setText("Accept Order");
                break;
            case "Processing":
                holder.btnSecondary.setVisibility(View.GONE);
                holder.btnPrimary.setText("Mark Shipped");
                break;
            case "Shipped":
                holder.btnSecondary.setVisibility(View.GONE);
                holder.btnPrimary.setVisibility(View.GONE);
                break;
            case "Delivered":
            case "Cancelled":
            case "Declined":
                holder.btnSecondary.setVisibility(View.GONE);
                holder.btnPrimary.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvCustomerName, tvOrderTotal;
        LinearLayout llOrderItemsContainer;
        MaterialButton btnPrimary, btnSecondary;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvCustomerName = itemView.findViewById(R.id.tvCustomerName);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            llOrderItemsContainer = itemView.findViewById(R.id.llOrderItemsContainer);
            btnPrimary = itemView.findViewById(R.id.btnPrimary);
            btnSecondary = itemView.findViewById(R.id.btnSecondary);
        }
    }
}

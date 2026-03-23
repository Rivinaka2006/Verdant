package lk.evolvex.rivinaka.verdant.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Order;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private List<Order> orders;
    private OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onTrackOrder(Order order);
        void onCancelOrder(Order order);
        void onRateProduct(CartItem item);
    }

    public OrderAdapter(List<Order> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orders.get(position);
        
        holder.tvOrderId.setText("Order ID: #" + (order.getOrderId().length() > 8 ? order.getOrderId().substring(0, 8) : order.getOrderId()));
        holder.tvStatus.setText(order.getStatus());
        holder.tvPrice.setText(String.format("Rs. %.2f", order.getTotalAmount()));

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
                MaterialButton btnRateProduct = itemView.findViewById(R.id.btnRateProduct);

                tvProductName.setText(item.getProductName());
                tvQuantity.setText("Qty = " + item.getQuantity());
                tvItemPrice.setText(String.format("Rs. %.2f", item.getProductPrice()));

                Glide.with(holder.itemView.getContext())
                        .load(item.getProductImage())
                        .placeholder(R.drawable.plant)
                        .into(ivProduct);

                // Show Rate button only if order is delivered
                if ("Delivered".equalsIgnoreCase(order.getStatus()) || "Completed".equalsIgnoreCase(order.getStatus())) {
                    btnRateProduct.setVisibility(View.VISIBLE);
                    btnRateProduct.setOnClickListener(v -> {
                        if (listener != null) listener.onRateProduct(item);
                    });
                } else {
                    btnRateProduct.setVisibility(View.GONE);
                }

                holder.llOrderItemsContainer.addView(itemView);
            }
        }

        // Status styling and bottom buttons
        if ("Cancelled".equalsIgnoreCase(order.getStatus())) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
            holder.btnActionLeft.setVisibility(View.GONE);
            holder.btnActionRight.setVisibility(View.GONE); // Requirement: remove Re-order
        } else if ("Delivered".equalsIgnoreCase(order.getStatus()) || "Completed".equalsIgnoreCase(order.getStatus())) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_green));
            holder.btnActionLeft.setVisibility(View.GONE); // Per requirement, we rate individual products now
            holder.btnActionRight.setVisibility(View.GONE); // Requirement: remove Re-order
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.app_green));
            holder.btnActionLeft.setVisibility(View.VISIBLE);
            holder.btnActionLeft.setText("Cancel Order");
            holder.btnActionRight.setVisibility(View.VISIBLE);
            holder.btnActionRight.setText("Track Order");
        }

        holder.btnActionRight.setOnClickListener(v -> {
            if (listener != null) listener.onTrackOrder(order);
        });

        holder.btnActionLeft.setOnClickListener(v -> {
            if (listener != null) listener.onCancelOrder(order);
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public void updateList(List<Order> newOrders) {
        this.orders = newOrders;
        notifyDataSetChanged();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvStatus, tvPrice, btnActionLeft, btnActionRight;
        LinearLayout llOrderItemsContainer;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            btnActionLeft = itemView.findViewById(R.id.btnActionLeft);
            btnActionRight = itemView.findViewById(R.id.btnActionRight);
            llOrderItemsContainer = itemView.findViewById(R.id.llOrderItemsContainer);
        }
    }
}

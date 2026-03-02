package lk.evolvex.rivinaka.verdant.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.CheckoutActivity;
import lk.evolvex.rivinaka.verdant.adapter.CartAdapter;
import lk.evolvex.rivinaka.verdant.model.CartItem;

public class CartFragment extends Fragment {

    private RecyclerView rvCartItems;
    private TextView tvSubtotal;
    private TextView tvShippingFee;
    private TextView tvTotal;
    private CartAdapter cartAdapter;
    private List<CartItem> cartItems;
    private final double SHIPPING_FEE = 100.00;
    private Button btnCheckout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCartItems = view.findViewById(R.id.recyclerCart);
        tvSubtotal = view.findViewById(R.id.tvSubtotal);
        tvShippingFee = view.findViewById(R.id.tvShippingFee);
        tvTotal = view.findViewById(R.id.tvTotal);
        btnCheckout = view.findViewById(R.id.btnCheckout);

        btnCheckout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), CheckoutActivity.class);
                startActivity(intent);
            }
        });


        cartItems = new ArrayList<>();
        // Add sample data
        cartItems.add(new CartItem("Peace Lily", "Rs 500", 1, R.drawable.person_icon));
        cartItems.add(new CartItem("Snake Plant", "Rs 750", 2, R.drawable.person_icon));
        cartItems.add(new CartItem("ZZ Plant", "Rs 900", 1, R.drawable.person_icon));
        cartItems.add(new CartItem("ZZ Plant", "Rs 900", 1, R.drawable.person_icon));
        cartItems.add(new CartItem("ZZ Plant", "Rs 900", 1, R.drawable.person_icon));
        cartItems.add(new CartItem("ZZ Plant", "Rs 900", 1, R.drawable.person_icon));
        cartItems.add(new CartItem("ZZ Plant", "Rs 900", 1, R.drawable.person_icon));
        cartItems.add(new CartItem("ZZ Plant", "Rs 900", 1, R.drawable.person_icon));
        cartItems.add(new CartItem("ZZ Plant", "Rs 900", 1, R.drawable.person_icon));


        cartAdapter = new CartAdapter(cartItems);
        rvCartItems.setLayoutManager(new LinearLayoutManager(getContext()));
        rvCartItems.setAdapter(cartAdapter);

        updateTotals();
    }

    private void updateTotals() {
        double subtotal = 0;
        for (CartItem item : cartItems) {
            // Assuming the price is in the format "Rs XXX"
            String priceString = item.getProductPrice().replace("Rs ", "");
            subtotal += Double.parseDouble(priceString) * item.getQuantity();
        }
        double total = subtotal + SHIPPING_FEE;

        tvSubtotal.setText(String.format("Rs %.2f", subtotal));
        tvShippingFee.setText(String.format("Rs %.2f", SHIPPING_FEE));
        tvTotal.setText(String.format("Rs %.2f", total));
    }
}

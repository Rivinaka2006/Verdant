package lk.evolvex.rivinaka.verdant.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.activity.AddEditProductActivity;
import lk.evolvex.rivinaka.verdant.activity.SellerMainHome;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Order;

public class SellerDashboardFragment extends Fragment {

    private TextView tvTodayOrders, tvRevenueToday, tvLowStock, tvPendingDeliveries;
    private LineChart salesChart;
    private FirebaseFirestore db;
    private String sellerId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            sellerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        initViews(view);
        setupChart();
        setupClickListeners(view);
        
        if (sellerId != null) {
            loadDashboardData();
        }
    }

    private void initViews(View view) {
        tvTodayOrders = view.findViewById(R.id.tvTodayOrders);
        tvRevenueToday = view.findViewById(R.id.tvRevenueToday);
        tvLowStock = view.findViewById(R.id.tvLowStock);
        tvPendingDeliveries = view.findViewById(R.id.tvPendingDeliveries);
        salesChart = view.findViewById(R.id.salesChart);
    }

    private void setupChart() {
        salesChart.getDescription().setEnabled(false);
        salesChart.setDrawGridBackground(false);
        salesChart.getLegend().setEnabled(false);
        salesChart.setTouchEnabled(true);
        salesChart.setDragEnabled(true);
        salesChart.setScaleEnabled(true);
        salesChart.setPinchZoom(true);

        XAxis xAxis = salesChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        salesChart.getAxisLeft().setTextColor(Color.WHITE);
        salesChart.getAxisLeft().setDrawGridLines(true);
        salesChart.getAxisLeft().setGridColor(Color.parseColor("#33FFFFFF"));
        salesChart.getAxisRight().setEnabled(false);
    }

    private void setupClickListeners(View view) {
        view.findViewById(R.id.btnAddNewPlant).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddEditProductActivity.class);
            startActivity(intent);
        });

        view.findViewById(R.id.btnUpdateStock).setOnClickListener(v -> {
            if (getActivity() instanceof SellerMainHome) {
                ((SellerMainHome) getActivity()).selectTab(R.id.nav_seller_products);
            }
        });

        view.findViewById(R.id.btnViewOrders).setOnClickListener(v -> {
            if (getActivity() instanceof SellerMainHome) {
                ((SellerMainHome) getActivity()).selectTab(R.id.nav_seller_orders);
            }
        });
    }

    private void loadDashboardData() {
        loadLowStockCount();
        loadOrdersAndSalesData();
    }

    private void loadLowStockCount() {
        db.collection("products")
                .whereEqualTo("nurseryId", sellerId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int lowStockCount = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long stock = doc.getLong("stock");
                        if (stock != null && stock < 5) {
                            lowStockCount++;
                        }
                    }
                    tvLowStock.setText(String.valueOf(lowStockCount));
                })
                .addOnFailureListener(e -> tvLowStock.setText("0"));
    }

    private void loadOrdersAndSalesData() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp todayStart = new Timestamp(cal.getTime());

        // Get 7 days ago
        cal.add(Calendar.DAY_OF_YEAR, -6);
        Timestamp sevenDaysAgoStart = new Timestamp(cal.getTime());

        db.collection("products")
                .whereEqualTo("nurseryId", sellerId)
                .get()
                .addOnSuccessListener(productSnapshots -> {
                    Set<String> myProductIds = new HashSet<>();
                    for (QueryDocumentSnapshot doc : productSnapshots) {
                        myProductIds.add(doc.getId());
                    }

                    if (myProductIds.isEmpty()) {
                        updateUIZero();
                        showEmptyChart();
                        return;
                    }

                    processOrdersData(todayStart, sevenDaysAgoStart, myProductIds);
                })
                .addOnFailureListener(e -> {
                    updateUIZero();
                    showEmptyChart();
                });
    }

    private void processOrdersData(Timestamp todayStart, Timestamp sevenDaysAgoStart, Set<String> myProductIds) {
        db.collection("orders")
                .whereGreaterThanOrEqualTo("createdAt", sevenDaysAgoStart)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int todayOrdersCount = 0;
                    double todayRevenue = 0;
                    
                    Map<String, Double> dailyRevenueMap = new HashMap<>();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                    
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(sevenDaysAgoStart.toDate());
                    List<String> lastSevenDays = new ArrayList<>();
                    for (int i = 0; i < 7; i++) {
                        String dayLabel = dateFormat.format(cal.getTime());
                        lastSevenDays.add(dayLabel);
                        dailyRevenueMap.put(dayLabel, 0.0);
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        if (order.getCreatedAt() == null) continue;

                        boolean hasMyProduct = false;
                        double orderRevenueFromMe = 0;

                        if (order.getItems() != null) {
                            for (CartItem item : order.getItems()) {
                                if (myProductIds.contains(item.getProductId())) {
                                    hasMyProduct = true;
                                    orderRevenueFromMe += (item.getProductPrice() * item.getQuantity());
                                }
                            }
                        }

                        if (hasMyProduct) {
                            if (order.getCreatedAt().getSeconds() >= todayStart.getSeconds()) {
                                todayOrdersCount++;
                                todayRevenue += orderRevenueFromMe;
                            }

                            String dayLabel = dateFormat.format(order.getCreatedAt().toDate());
                            if (dailyRevenueMap.containsKey(dayLabel)) {
                                dailyRevenueMap.put(dayLabel, dailyRevenueMap.get(dayLabel) + orderRevenueFromMe);
                            }
                        }
                    }

                    tvTodayOrders.setText(String.valueOf(todayOrdersCount));
                    tvRevenueToday.setText(String.format("LKR %.2f", todayRevenue));
                    
                    updateChart(lastSevenDays, dailyRevenueMap);
                });

        db.collection("orders")
                .whereIn("status", Arrays.asList("Pending", "Processing", "Shipped"))
                .get()
                .addOnSuccessListener(pendingSnapshots -> {
                    int pendingDeliveriesCount = 0;
                    for (QueryDocumentSnapshot doc : pendingSnapshots) {
                        Order order = doc.toObject(Order.class);
                        boolean hasMyProduct = false;

                        if (order.getItems() != null) {
                            for (CartItem item : order.getItems()) {
                                if (myProductIds.contains(item.getProductId())) {
                                    hasMyProduct = true;
                                    break;
                                }
                            }
                        }

                        if (hasMyProduct) {
                            pendingDeliveriesCount++;
                        }
                    }
                    tvPendingDeliveries.setText(String.valueOf(pendingDeliveriesCount));
                });
    }

    private void updateChart(List<String> dates, Map<String, Double> dailyRevenue) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            String date = dates.get(i);
            float val = dailyRevenue.get(date).floatValue();
            entries.add(new Entry(i, val));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Revenue");
        dataSet.setColor(Color.parseColor("#00894D")); // app_green
        dataSet.setCircleColor(Color.parseColor("#00894D"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(9f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#00894D"));
        dataSet.setFillAlpha(50);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        salesChart.setData(lineData);
        
        salesChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(dates));
        salesChart.invalidate();
    }

    private void showEmptyChart() {
        salesChart.clear();
        salesChart.setNoDataText("No sales data available");
        salesChart.setNoDataTextColor(Color.WHITE);
        salesChart.invalidate();
    }

    private void updateUIZero() {
        tvTodayOrders.setText("0");
        tvRevenueToday.setText("LKR 0.00");
        tvPendingDeliveries.setText("0");
    }
}

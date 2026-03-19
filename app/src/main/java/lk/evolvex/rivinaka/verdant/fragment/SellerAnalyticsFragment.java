package lk.evolvex.rivinaka.verdant.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import lk.evolvex.rivinaka.verdant.R;
import lk.evolvex.rivinaka.verdant.model.CartItem;
import lk.evolvex.rivinaka.verdant.model.Order;

public class SellerAnalyticsFragment extends Fragment {

    private LineChart revenueTrendChart;
    private HorizontalBarChart topSellingChart;
    private PieChart categoryPieChart;
    private MaterialButtonToggleGroup toggleFilter;

    private FirebaseFirestore db;
    private String sellerId;
    private int currentFilterDays = 7;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_seller_analytics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            sellerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        initViews(view);
        setupCharts();
        setupFilter();

        if (sellerId != null) {
            loadAnalyticsData();
        }
    }

    private void initViews(View view) {
        revenueTrendChart = view.findViewById(R.id.revenueTrendChart);
        topSellingChart = view.findViewById(R.id.topSellingChart);
        categoryPieChart = view.findViewById(R.id.categoryPieChart);
        toggleFilter = view.findViewById(R.id.toggleFilter);
    }

    private void setupCharts() {
        // Setup Revenue Chart
        revenueTrendChart.getDescription().setEnabled(false);
        revenueTrendChart.getLegend().setTextColor(Color.WHITE);
        revenueTrendChart.getXAxis().setTextColor(Color.WHITE);
        revenueTrendChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        revenueTrendChart.getAxisLeft().setTextColor(Color.WHITE);
        revenueTrendChart.getAxisRight().setEnabled(false);
        revenueTrendChart.setNoDataTextColor(Color.WHITE);

        // Setup Top Selling Chart
        topSellingChart.getDescription().setEnabled(false);
        topSellingChart.getLegend().setTextColor(Color.WHITE);
        topSellingChart.getXAxis().setTextColor(Color.WHITE);
        topSellingChart.getAxisLeft().setTextColor(Color.WHITE);
        topSellingChart.getAxisRight().setEnabled(false);
        topSellingChart.setNoDataTextColor(Color.WHITE);

        // Setup Pie Chart
        categoryPieChart.getDescription().setEnabled(false);
        categoryPieChart.getLegend().setTextColor(Color.WHITE);
        categoryPieChart.setHoleColor(Color.TRANSPARENT);
        categoryPieChart.setCenterTextColor(Color.WHITE);
        categoryPieChart.setEntryLabelColor(Color.WHITE);
        categoryPieChart.setNoDataTextColor(Color.WHITE);
        categoryPieChart.setDrawEntryLabels(false);
    }

    private void setupFilter() {
        toggleFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnToday) currentFilterDays = 1;
                else if (checkedId == R.id.btn7Days) currentFilterDays = 7;
                else if (checkedId == R.id.btn30Days) currentFilterDays = 30;

                loadAnalyticsData();
            }
        });
    }

    private void loadAnalyticsData() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        
        if (currentFilterDays > 1) {
            cal.add(Calendar.DAY_OF_YEAR, -(currentFilterDays - 1));
        }
        Timestamp startTime = new Timestamp(cal.getTime());

        db.collection("products")
                .whereEqualTo("nurseryId", sellerId)
                .get()
                .addOnSuccessListener(productSnapshots -> {
                    Set<String> myProductIds = new HashSet<>();
                    Map<String, String> productCategoryMap = new HashMap<>();
                    Map<String, String> productNameMap = new HashMap<>();
                    
                    for (QueryDocumentSnapshot doc : productSnapshots) {
                        String id = doc.getId();
                        myProductIds.add(id);
                        productCategoryMap.put(id, doc.getString("category"));
                        productNameMap.put(id, doc.getString("name"));
                    }

                    if (myProductIds.isEmpty()) {
                        showEmptyCharts();
                        return;
                    }

                    processOrdersForAnalytics(startTime, myProductIds, productCategoryMap, productNameMap);
                });
    }

    private void processOrdersForAnalytics(Timestamp startTime, Set<String> myProductIds, 
                                          Map<String, String> productCategoryMap, 
                                          Map<String, String> productNameMap) {
        
        db.collection("orders")
                .whereGreaterThanOrEqualTo("createdAt", startTime)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Double> dailyRevenue = new HashMap<>();
                    Map<String, Integer> productSalesCount = new HashMap<>();
                    Map<String, Integer> categorySalesCount = new HashMap<>();
                    
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                    
                    // Initialize Daily Revenue Map
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(startTime.toDate());
                    List<String> dateLabels = new ArrayList<>();
                    for (int i = 0; i < currentFilterDays; i++) {
                        String label = dateFormat.format(cal.getTime());
                        dateLabels.add(label);
                        dailyRevenue.put(label, 0.0);
                        cal.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        if (order.getCreatedAt() == null) continue;

                        String dateLabel = dateFormat.format(order.getCreatedAt().toDate());
                        
                        if (order.getItems() != null) {
                            for (CartItem item : order.getItems()) {
                                if (myProductIds.contains(item.getProductId())) {
                                    double itemRevenue = item.getProductPrice() * item.getQuantity();
                                    
                                    // Update Revenue
                                    if (dailyRevenue.containsKey(dateLabel)) {
                                        dailyRevenue.put(dateLabel, dailyRevenue.get(dateLabel) + itemRevenue);
                                    }
                                    
                                    // Update Product Sales
                                    String pId = item.getProductId();
                                    productSalesCount.put(pId, productSalesCount.getOrDefault(pId, 0) + item.getQuantity());
                                    
                                    // Update Category Sales
                                    String cat = productCategoryMap.get(pId);
                                    if (cat != null) {
                                        categorySalesCount.put(cat, categorySalesCount.getOrDefault(cat, 0) + item.getQuantity());
                                    }
                                }
                            }
                        }
                    }

                    updateRevenueChart(dateLabels, dailyRevenue);
                    updateTopSellingChart(productSalesCount, productNameMap);
                    updateCategoryChart(categorySalesCount);
                });
    }

    private void updateRevenueChart(List<String> labels, Map<String, Double> data) {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            entries.add(new Entry(i, data.get(labels.get(i)).floatValue()));
        }

        LineDataSet set = new LineDataSet(entries, "Revenue (LKR)");
        set.setColor(Color.parseColor("#00894D"));
        set.setCircleColor(Color.parseColor("#00894D"));
        set.setLineWidth(2f);
        set.setDrawFilled(true);
        set.setFillColor(Color.parseColor("#00894D"));
        set.setFillAlpha(50);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setValueTextColor(Color.WHITE);

        revenueTrendChart.setData(new LineData(set));
        revenueTrendChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        revenueTrendChart.invalidate();
    }

    private void updateTopSellingChart(Map<String, Integer> sales, Map<String, String> names) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(sales.entrySet());
        list.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        int count = Math.min(list.size(), 5);
        for (int i = 0; i < count; i++) {
            Map.Entry<String, Integer> entry = list.get(i);
            entries.add(new BarEntry(i, entry.getValue()));
            labels.add(names.get(entry.getKey()));
        }

        BarDataSet set = new BarDataSet(entries, "Units Sold");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextColor(Color.WHITE);

        topSellingChart.setData(new BarData(set));
        topSellingChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        topSellingChart.invalidate();
    }

    private void updateCategoryChart(Map<String, Integer> categorySales) {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : categorySales.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.JOYFUL_COLORS);
        set.setValueTextColor(Color.WHITE);
        set.setValueTextSize(12f);
        set.setDrawValues(false); // Remove value text from slices

        categoryPieChart.setData(new PieData(set));
        categoryPieChart.invalidate();
    }

    private void showEmptyCharts() {
        revenueTrendChart.clear();
        topSellingChart.clear();
        categoryPieChart.clear();
        revenueTrendChart.setNoDataText("No data available");
        topSellingChart.setNoDataText("No data available");
        categoryPieChart.setNoDataText("No data available");
        revenueTrendChart.invalidate();
        topSellingChart.invalidate();
        categoryPieChart.invalidate();
    }
}

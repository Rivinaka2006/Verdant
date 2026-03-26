package lk.evolvex.rivinaka.verdant.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import lk.evolvex.rivinaka.verdant.R;

public class RecentSearchAdapter extends RecyclerView.Adapter<RecentSearchAdapter.ViewHolder> {

    private List<String> recentSearches;
    private OnRecentSearchClickListener listener;

    public interface OnRecentSearchClickListener {
        void onRecentSearchClick(String query);
        void onRemoveRecentSearch(int position);
    }

    public RecentSearchAdapter(List<String> recentSearches, OnRecentSearchClickListener listener) {
        this.recentSearches = recentSearches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = recentSearches.get(position);
        holder.tvRecentQuery.setText(query);
        holder.itemView.setOnClickListener(v -> listener.onRecentSearchClick(query));
        holder.btnRemoveRecent.setOnClickListener(v -> listener.onRemoveRecentSearch(position));
    }

    @Override
    public int getItemCount() {
        return recentSearches.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecentQuery;
        ImageView btnRemoveRecent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecentQuery = itemView.findViewById(R.id.tvRecentQuery);
            btnRemoveRecent = itemView.findViewById(R.id.btnRemoveRecent);
        }
    }
}
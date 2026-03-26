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
import lk.evolvex.rivinaka.verdant.model.Banner;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private List<Banner> bannerList;

    public BannerAdapter(List<Banner> bannerList) {
        this.bannerList = bannerList;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        if (bannerList == null || bannerList.isEmpty()) return;
        
        // Use modulo to get the correct item for infinite sliding
        Banner banner = bannerList.get(position % bannerList.size());
        
        String title = banner.getTitle();
        String subtitle = banner.getSubtitle();
        
        boolean hasContent = (title != null && !title.trim().isEmpty()) || 
                             (subtitle != null && !subtitle.trim().isEmpty());
        
        if (hasContent) {
            holder.vOverlay.setVisibility(View.VISIBLE);
            holder.tvTitle.setVisibility(View.VISIBLE);
            holder.tvSubtitle.setVisibility(View.VISIBLE);
            holder.tvTitle.setText(title);
            holder.tvSubtitle.setText(subtitle);
        } else {
            holder.vOverlay.setVisibility(View.GONE);
            holder.tvTitle.setVisibility(View.GONE);
            holder.tvSubtitle.setVisibility(View.GONE);
        }
        
        Glide.with(holder.itemView.getContext())
                .load(banner.getImageUrl())
                .into(holder.ivBanner);
    }

    @Override
    public int getItemCount() {
        // Return a very large number to simulate infinite sliding
        return bannerList == null || bannerList.isEmpty() ? 0 : Integer.MAX_VALUE;
    }

    public int getRealCount() {
        return bannerList == null ? 0 : bannerList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBanner;
        TextView tvTitle, tvSubtitle;
        View vOverlay;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBanner = itemView.findViewById(R.id.ivBanner);
            tvTitle = itemView.findViewById(R.id.tvBannerTitle);
            tvSubtitle = itemView.findViewById(R.id.tvBannerSubtitle);
            vOverlay = itemView.findViewById(R.id.vOverlay);
        }
    }
}

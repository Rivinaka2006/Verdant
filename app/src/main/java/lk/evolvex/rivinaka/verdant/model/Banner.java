package lk.evolvex.rivinaka.verdant.model;

import com.google.firebase.Timestamp;

public class Banner {
    private String title;
    private String subtitle;
    private String imageUrl;
    private String link;
    private int order;
    private boolean active;
    private Timestamp createdAt;

    public Banner() {
    }

    public Banner(String title, String subtitle, String imageUrl, String link, int order, boolean active, Timestamp createdAt) {
        this.title = title;
        this.subtitle = subtitle;
        this.imageUrl = imageUrl;
        this.link = link;
        this.order = order;
        this.active = active;
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}

package lk.evolvex.rivinaka.verdant.model;

import com.google.firebase.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    private String id;
    private String title;
    private String body;
    private Timestamp timestamp;
    private boolean read;
}

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
public class User {

    private String userId;

    private String fullName;

    private String email;

    private String phone;

    private String role;

    private String profileImageUrl;

    private String defaultAddressId;

    private String fcmToken;

    private Boolean biometricEnabled;

    private Timestamp createdAt;

}

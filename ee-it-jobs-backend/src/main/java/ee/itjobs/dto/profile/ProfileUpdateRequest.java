package ee.itjobs.dto.profile;

import lombok.Data;

@Data
public class ProfileUpdateRequest {
    private String firstName;
    private String lastName;
    private String phone;
    private String linkedinUrl;
    private String coverLetter;
    private Boolean emailAlerts;
    private Integer alertThreshold;
    private java.util.Map<String, Object> notificationPreferences;
}

package ee.itjobs.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProfileDto {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String linkedinUrl;
    private String coverLetter;
    private List<String> skills;
    private Map<String, Object> preferences;
    private String cvRawText;
    private Integer yearsExperience;
    private String roleLevel;
    private boolean hasCv;
    private Boolean emailAlerts;
    private Integer alertThreshold;
    private Map<String, Object> notificationPreferences;
}

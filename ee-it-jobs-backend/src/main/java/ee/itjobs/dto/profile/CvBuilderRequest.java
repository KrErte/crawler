package ee.itjobs.dto.profile;

import lombok.Data;
import java.util.List;

@Data
public class CvBuilderRequest {
    private String fullName;
    private String email;
    private String phone;
    private String linkedinUrl;
    private String summary;
    private List<ExperienceEntry> experience;
    private List<EducationEntry> education;
    private List<String> skills;

    @Data
    public static class ExperienceEntry {
        private String company;
        private String role;
        private String startDate;
        private String endDate;
        private String description;
    }

    @Data
    public static class EducationEntry {
        private String institution;
        private String degree;
        private String field;
        private String startDate;
        private String endDate;
    }
}

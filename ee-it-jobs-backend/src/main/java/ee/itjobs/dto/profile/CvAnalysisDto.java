package ee.itjobs.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CvAnalysisDto {
    private int completenessScore;
    private List<String> detectedSkills;
    private List<String> missingInDemandSkills;
    private List<String> suggestions;
    private Integer yearsExperience;
    private String roleLevel;
    private int totalActiveJobs;
    private int matchingJobs;
}

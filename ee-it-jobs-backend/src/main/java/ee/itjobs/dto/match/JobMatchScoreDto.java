package ee.itjobs.dto.match;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class JobMatchScoreDto {
    private Long jobId;
    private int matchPercentage;
    private List<String> matchedSkills;
}

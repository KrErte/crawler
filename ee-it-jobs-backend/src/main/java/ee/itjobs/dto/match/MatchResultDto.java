package ee.itjobs.dto.match;

import ee.itjobs.dto.job.JobDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MatchResultDto {
    private JobDto job;
    private int matchPercentage;
    private List<String> matchedSkills;
    private String matchExplanation;
}

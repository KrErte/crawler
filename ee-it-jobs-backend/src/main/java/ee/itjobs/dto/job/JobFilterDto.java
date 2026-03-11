package ee.itjobs.dto.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data @Builder @AllArgsConstructor
public class JobFilterDto {
    private List<String> companies;
    private List<String> sources;
    private List<String> jobTypes;
    private List<String> workplaceTypes;
}

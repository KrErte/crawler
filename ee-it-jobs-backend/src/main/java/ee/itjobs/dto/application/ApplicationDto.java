package ee.itjobs.dto.application;

import ee.itjobs.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ApplicationDto {
    private Long id;
    private Long jobId;
    private String jobTitle;
    private String company;
    private String jobUrl;
    private String source;
    private ApplicationStatus status;
    private String notes;
    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
}

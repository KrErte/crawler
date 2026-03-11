package ee.itjobs.dto.application;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateApplicationRequest {
    @NotNull
    private Long jobId;
    private String notes;
}

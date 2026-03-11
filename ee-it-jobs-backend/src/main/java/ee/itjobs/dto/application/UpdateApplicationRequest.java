package ee.itjobs.dto.application;

import ee.itjobs.enums.ApplicationStatus;
import lombok.Data;

@Data
public class UpdateApplicationRequest {
    private ApplicationStatus status;
    private String notes;
}

package ee.itjobs.dto.admin;

import lombok.Data;

import java.util.List;

@Data
public class JobUpdateRequest {
    private String title;
    private String company;
    private String description;
    private List<String> skills;
    private Boolean isActive;
}

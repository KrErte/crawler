package ee.itjobs.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Boolean isActive;
    private Boolean isAdmin;
    private LocalDateTime createdAt;
    private Long applicationCount;
}

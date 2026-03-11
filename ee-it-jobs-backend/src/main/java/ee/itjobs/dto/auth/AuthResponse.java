package ee.itjobs.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data @Builder @AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private UserDto user;
    private Boolean requiresTwoFactor;

    @Data @Builder @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private Boolean isAdmin;
    }
}

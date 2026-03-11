package ee.itjobs.dto.auth;

import lombok.Data;

@Data
public class TotpVerifyRequest {
    private String code;
}

package ee.itjobs.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TotpSetupResponse {
    private String secret;
    private String qrCodeDataUri;
}

package ee.itjobs.security;

import ee.itjobs.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private static final String SECRET = "a-very-long-secret-key-that-is-at-least-64-characters-long-for-hmac-sha-256-algorithm";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours
    private static final long REFRESH_EXPIRATION_MS = 604800000L; // 7 days

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        lenient().when(jwtConfig.getSecret()).thenReturn(SECRET);
        lenient().when(jwtConfig.getExpirationMs()).thenReturn(EXPIRATION_MS);
        lenient().when(jwtConfig.getRefreshExpirationMs()).thenReturn(REFRESH_EXPIRATION_MS);
    }

    @Test
    void generateToken_andExtractEmail_returnsCorrectEmail() {
        // Arrange
        String email = "user@example.com";

        // Act
        String token = jwtTokenProvider.generateToken(email);
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(email, extractedEmail);
    }

    @Test
    void generateRefreshToken_containsTypeClaim() {
        // Arrange
        String email = "user@example.com";

        // Act
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);

        // Assert
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());

        // Parse the token to verify the "type" claim
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(refreshToken)
                .getPayload();

        assertEquals(email, claims.getSubject());
        assertEquals("refresh", claims.get("type", String.class));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        // Arrange
        String email = "valid@example.com";
        String token = jwtTokenProvider.generateToken(email);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_expiredToken_returnsFalse() {
        // Arrange - generate token with 0ms expiration (already expired)
        when(jwtConfig.getExpirationMs()).thenReturn(0L);
        String email = "expired@example.com";
        String token = jwtTokenProvider.generateToken(email);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        // Arrange
        String email = "user@example.com";
        String token = jwtTokenProvider.generateToken(email);
        // Tamper with the token by modifying the payload section
        String tamperedToken = token.substring(0, token.lastIndexOf('.')) + ".invalidSignature";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        // Arrange
        String malformedToken = "this.is.not.a.valid.jwt";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void validateToken_emptyString_returnsFalse() {
        // Arrange
        String emptyToken = "";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(emptyToken);

        // Assert
        assertFalse(isValid);
    }

    @Test
    void generateToken_differentEmails_produceDifferentTokens() {
        // Arrange
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";

        // Act
        String token1 = jwtTokenProvider.generateToken(email1);
        String token2 = jwtTokenProvider.generateToken(email2);

        // Assert
        assertNotEquals(token1, token2);
        assertEquals(email1, jwtTokenProvider.getEmailFromToken(token1));
        assertEquals(email2, jwtTokenProvider.getEmailFromToken(token2));
    }

    @Test
    void generateRefreshToken_extractEmail_returnsCorrectEmail() {
        // Arrange
        String email = "refresh@example.com";

        // Act
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);
        String extractedEmail = jwtTokenProvider.getEmailFromToken(refreshToken);

        // Assert
        assertEquals(email, extractedEmail);
    }

    @Test
    void validateToken_validRefreshToken_returnsTrue() {
        // Arrange
        String email = "user@example.com";
        String refreshToken = jwtTokenProvider.generateRefreshToken(email);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(refreshToken);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void validateToken_tokenSignedWithDifferentKey_returnsFalse() {
        // Arrange - generate a token with a different secret
        String differentSecret = "a-completely-different-secret-key-that-is-also-at-least-64-characters-long-for-testing";
        SecretKey differentKey = Keys.hmacShaKeyFor(differentSecret.getBytes(StandardCharsets.UTF_8));

        String tokenWithDifferentKey = Jwts.builder()
                .subject("user@example.com")
                .issuedAt(new java.util.Date())
                .expiration(new java.util.Date(System.currentTimeMillis() + 86400000L))
                .signWith(differentKey)
                .compact();

        // Act
        boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentKey);

        // Assert
        assertFalse(isValid);
    }
}

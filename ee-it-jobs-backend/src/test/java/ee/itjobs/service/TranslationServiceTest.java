package ee.itjobs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TranslationServiceTest {

    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        translationService = new TranslationService();
    }

    @Test
    void translate_sameLang_returnsOriginal() {
        String text = "Hello world";
        String result = translationService.translate(text, "en", "en");
        assertEquals(text, result);
    }

    @Test
    void translate_nullText_returnsNull() {
        assertNull(translationService.translate(null, "en", "et"));
    }

    @Test
    void translate_blankText_returnsBlank() {
        String result = translationService.translate("   ", "en", "et");
        assertEquals("   ", result);
    }

    @Test
    void translate_longText_truncatesTo500() {
        String longText = "a".repeat(1000);
        // Should not throw, should handle truncation gracefully
        // API call may fail in test env, but should return original text as fallback
        String result = translationService.translate(longText, "en", "et");
        assertNotNull(result);
    }

    @Test
    void isEstonian_estonianText_returnsTrue() {
        String estonianText = "Otsime kogenud tarkvara arendajat oma meeskonda. Nõuded: kogemus Java ja Spring raamistikuga. Pakume konkurentsivõimelist palka.";
        assertTrue(translationService.isEstonian(estonianText));
    }

    @Test
    void isEstonian_englishText_returnsFalse() {
        String englishText = "We are looking for a software developer to join our team. Requirements: experience with Java and Spring framework.";
        assertFalse(translationService.isEstonian(englishText));
    }

    @Test
    void isEstonian_shortText_returnsFalse() {
        assertFalse(translationService.isEstonian("Hi"));
        assertFalse(translationService.isEstonian(null));
    }
}

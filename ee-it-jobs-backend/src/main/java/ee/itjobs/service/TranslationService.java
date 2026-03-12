package ee.itjobs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@Slf4j
public class TranslationService {

    private final WebClient webClient;

    public TranslationService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.mymemory.translated.net")
                .build();
    }

    @Cacheable(value = "job-translations", key = "{#sourceLang, #targetLang, #text?.hashCode()}")
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) return text;
        if (sourceLang.equals(targetLang)) return text;

        // Truncate very long texts
        String input = text.length() > 500 ? text.substring(0, 500) : text;

        try {
            String langPair = mapLang(sourceLang) + "|" + mapLang(targetLang);
            String result = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/get")
                            .queryParam("q", input)
                            .queryParam("langpair", langPair)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .map(response -> {
                        Map<String, Object> responseData = (Map<String, Object>) response.get("responseData");
                        if (responseData != null) {
                            return (String) responseData.get("translatedText");
                        }
                        return input;
                    })
                    .block();

            if (result != null && !result.isBlank()) {
                return result;
            }
        } catch (Exception e) {
            log.warn("Translation failed for '{}': {}", input.substring(0, Math.min(50, input.length())), e.getMessage());
        }
        return text;
    }

    /**
     * Detect if text is likely Estonian.
     */
    public boolean isEstonian(String text) {
        if (text == null || text.length() < 10) return false;
        String lower = text.toLowerCase();
        // Common Estonian words and characters
        String[] estonianMarkers = {"ning", "või", "oma", "kui", "see", "mis", "kes",
                "töö", "kogemus", "arendaja", "nõuded", "pakume", "ootame",
                "ettevõte", "meeskond", "töökoht", "kandidaat", "oskused"};
        int matches = 0;
        for (String marker : estonianMarkers) {
            if (lower.contains(marker)) matches++;
        }
        // Check for Estonian special characters
        if (lower.contains("õ") || lower.contains("ä") || lower.contains("ö") || lower.contains("ü")) {
            matches += 2;
        }
        return matches >= 3;
    }

    private String mapLang(String lang) {
        return switch (lang) {
            case "et" -> "et";
            case "en" -> "en";
            default -> lang;
        };
    }
}

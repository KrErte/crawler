package ee.itjobs.scraper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SalaryParser {

    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "(\\d[\\d\\s.,]*)\\s*[-–—]\\s*(\\d[\\d\\s.,]*)\\s*([€$£]|EUR|USD|GBP)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CURRENCY_PREFIX = Pattern.compile(
            "([€$£])\\s*(\\d[\\d\\s.,]*)\\s*[-–—]?\\s*(\\d[\\d\\s.,]*)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SINGLE_VALUE = Pattern.compile(
            "(?:from|alates|min\\.?|vähemalt)\\s*(\\d[\\d\\s.,]*)\\s*([€$£]|EUR|USD|GBP)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UP_TO = Pattern.compile(
            "(?:up\\s+to|kuni|max\\.?)\\s*(\\d[\\d\\s.,]*)\\s*([€$£]|EUR|USD|GBP)?",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CURRENCY_SUFFIX = Pattern.compile(
            "([€$£]|EUR|USD|GBP|eur|usd|gbp)",
            Pattern.CASE_INSENSITIVE
    );

    public static ParsedSalary parse(String salaryText) {
        if (salaryText == null || salaryText.isBlank()) {
            return null;
        }

        String text = salaryText.trim();
        Integer min = null;
        Integer max = null;
        String currency = detectCurrency(text);

        // Try currency-prefix pattern: €3000-5000
        Matcher prefixMatcher = CURRENCY_PREFIX.matcher(text);
        if (prefixMatcher.find()) {
            currency = normalizeCurrency(prefixMatcher.group(1));
            min = parseNumber(prefixMatcher.group(2));
            if (prefixMatcher.group(3) != null) {
                max = parseNumber(prefixMatcher.group(3));
            } else {
                max = min;
            }
            return buildResult(min, max, currency);
        }

        // Try range pattern: 3000-5000 EUR
        Matcher rangeMatcher = RANGE_PATTERN.matcher(text);
        if (rangeMatcher.find()) {
            min = parseNumber(rangeMatcher.group(1));
            max = parseNumber(rangeMatcher.group(2));
            if (rangeMatcher.group(3) != null) {
                currency = normalizeCurrency(rangeMatcher.group(3));
            }
            return buildResult(min, max, currency);
        }

        // Try "from X" pattern
        Matcher fromMatcher = SINGLE_VALUE.matcher(text);
        if (fromMatcher.find()) {
            min = parseNumber(fromMatcher.group(1));
            if (fromMatcher.group(2) != null) {
                currency = normalizeCurrency(fromMatcher.group(2));
            }
            return buildResult(min, null, currency);
        }

        // Try "up to X" pattern
        Matcher upToMatcher = UP_TO.matcher(text);
        if (upToMatcher.find()) {
            max = parseNumber(upToMatcher.group(1));
            if (upToMatcher.group(2) != null) {
                currency = normalizeCurrency(upToMatcher.group(2));
            }
            return buildResult(null, max, currency);
        }

        // Fallback: try to find any number
        Matcher numberMatcher = Pattern.compile("(\\d[\\d\\s.,]*)").matcher(text);
        if (numberMatcher.find()) {
            int value = parseNumber(numberMatcher.group(1));
            if (value >= 500) { // Sanity check: must be a plausible salary
                return buildResult(value, value, currency);
            }
        }

        return null;
    }

    private static ParsedSalary buildResult(Integer min, Integer max, String currency) {
        if (min != null && max != null && min > max) {
            int temp = min;
            min = max;
            max = temp;
        }

        // Detect if values look like hourly (< 200) - convert to monthly estimate
        if (min != null && min < 200 && (max == null || max < 200)) {
            min = min * 168; // ~168 hours/month
            if (max != null) max = max * 168;
        }

        // Sanity check
        if (min != null && min < 100) return null;
        if (max != null && max > 500000) return null;

        return new ParsedSalary(min, max, currency != null ? currency : "EUR");
    }

    private static int parseNumber(String s) {
        if (s == null) return 0;
        String cleaned = s.replaceAll("[\\s,.]", "");
        // Handle cases like "3.000" or "3,000" (thousands separator)
        String original = s.trim();
        if (original.matches("\\d{1,3}[.,]\\d{3}")) {
            cleaned = original.replaceAll("[.,]", "");
        } else if (original.matches("\\d+[.,]\\d{1,2}")) {
            // Decimal like 3500.50 - just take integer part
            cleaned = original.split("[.,]")[0];
        }
        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String detectCurrency(String text) {
        Matcher m = CURRENCY_SUFFIX.matcher(text);
        if (m.find()) {
            return normalizeCurrency(m.group(1));
        }
        return "EUR"; // Default for Estonian market
    }

    private static String normalizeCurrency(String symbol) {
        if (symbol == null) return "EUR";
        return switch (symbol.toUpperCase()) {
            case "€", "EUR" -> "EUR";
            case "$", "USD" -> "USD";
            case "£", "GBP" -> "GBP";
            default -> "EUR";
        };
    }

    public record ParsedSalary(Integer min, Integer max, String currency) {}
}

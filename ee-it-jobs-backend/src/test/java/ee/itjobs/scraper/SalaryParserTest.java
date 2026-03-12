package ee.itjobs.scraper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SalaryParserTest {

    @Test
    void parse_euroRange_parsesCorrectly() {
        SalaryParser.ParsedSalary result = SalaryParser.parse("3000-5000 EUR");

        assertNotNull(result);
        assertEquals(3000, result.min());
        assertEquals(5000, result.max());
        assertEquals("EUR", result.currency());
    }

    @Test
    void parse_euroSymbolPrefix_parsesCorrectly() {
        SalaryParser.ParsedSalary result = SalaryParser.parse("€3000-5000");

        assertNotNull(result);
        assertEquals(3000, result.min());
        assertEquals(5000, result.max());
        assertEquals("EUR", result.currency());
    }

    @Test
    void parse_singleValueFrom_parsesMin() {
        SalaryParser.ParsedSalary result = SalaryParser.parse("from 3000 EUR");

        assertNotNull(result);
        assertEquals(3000, result.min());
        assertEquals("EUR", result.currency());
    }

    @Test
    void parse_upTo_parsesMax() {
        SalaryParser.ParsedSalary result = SalaryParser.parse("up to 5000€");

        assertNotNull(result);
        assertEquals(5000, result.max());
        assertEquals("EUR", result.currency());
    }

    @Test
    void parse_usdCurrency_detected() {
        SalaryParser.ParsedSalary result = SalaryParser.parse("$4000-6000");

        assertNotNull(result);
        assertEquals(4000, result.min());
        assertEquals(6000, result.max());
        assertEquals("USD", result.currency());
    }

    @Test
    void parse_thousandsSeparator_handled() {
        SalaryParser.ParsedSalary result = SalaryParser.parse("€3.000-5.000");

        assertNotNull(result);
        assertEquals(3000, result.min());
        assertEquals(5000, result.max());
    }

    @Test
    void parse_null_returnsNull() {
        assertNull(SalaryParser.parse(null));
    }

    @Test
    void parse_blankString_returnsNull() {
        assertNull(SalaryParser.parse("   "));
    }

    @Test
    void parse_unparseable_returnsNull() {
        assertNull(SalaryParser.parse("Competitive salary"));
    }

    @Test
    void parse_estonianFrom_parsed() {
        SalaryParser.ParsedSalary result = SalaryParser.parse("alates 2500 EUR");

        assertNotNull(result);
        assertEquals(2500, result.min());
        assertEquals("EUR", result.currency());
    }

    @Test
    void parse_swappedMinMax_normalized() {
        SalaryParser.ParsedSalary result = SalaryParser.parse("€5000-3000");

        assertNotNull(result);
        assertTrue(result.min() <= result.max());
    }
}

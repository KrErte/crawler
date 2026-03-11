package ee.itjobs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private StatsService statsService;

    // --- getTopSkills ---

    @Test
    void getTopSkills_returnsSkillsWithCounts() {
        // Arrange
        int limit = 5;
        List<Map<String, Object>> mockResult = List.of(
                Map.of("skill", "java", "count", 120L),
                Map.of("skill", "python", "count", 95L),
                Map.of("skill", "react", "count", 78L),
                Map.of("skill", "docker", "count", 65L),
                Map.of("skill", "kubernetes", "count", 42L)
        );

        when(jdbcTemplate.queryForList(anyString(), eq(limit))).thenReturn(mockResult);

        // Act
        List<Map<String, Object>> result = statsService.getTopSkills(limit);

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());
        assertEquals("java", result.get(0).get("skill"));
        assertEquals(120L, result.get(0).get("count"));
        assertEquals("python", result.get(1).get("skill"));
        assertEquals(95L, result.get(1).get("count"));
        assertEquals("kubernetes", result.get(4).get("skill"));
        assertEquals(42L, result.get(4).get("count"));

        verify(jdbcTemplate).queryForList(anyString(), eq(limit));
    }

    @Test
    void getTopSkills_emptyResult_returnsEmptyList() {
        // Arrange
        int limit = 10;
        when(jdbcTemplate.queryForList(anyString(), eq(limit)))
                .thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = statsService.getTopSkills(limit);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(jdbcTemplate).queryForList(anyString(), eq(limit));
    }

    @Test
    void getTopSkills_passesCorrectLimit() {
        // Arrange
        int limit = 3;
        when(jdbcTemplate.queryForList(anyString(), eq(limit)))
                .thenReturn(List.of(
                        Map.of("skill", "java", "count", 100L),
                        Map.of("skill", "python", "count", 80L),
                        Map.of("skill", "react", "count", 60L)
                ));

        // Act
        List<Map<String, Object>> result = statsService.getTopSkills(limit);

        // Assert
        assertEquals(3, result.size());

        verify(jdbcTemplate).queryForList(
                eq("SELECT skill, COUNT(*) as count FROM jobs, jsonb_array_elements_text(skills) AS skill " +
                   "WHERE is_active = true GROUP BY skill ORDER BY count DESC LIMIT ?"),
                eq(limit));
    }

    // --- getJobsBySource ---

    @Test
    void getJobsBySource_returnsSourceCounts() {
        // Arrange
        List<Map<String, Object>> mockResult = List.of(
                Map.of("source", "cvkeskus", "count", 350L),
                Map.of("source", "cv.ee", "count", 280L),
                Map.of("source", "linkedin", "count", 150L)
        );

        when(jdbcTemplate.queryForList(anyString())).thenReturn(mockResult);

        // Act
        List<Map<String, Object>> result = statsService.getJobsBySource();

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("cvkeskus", result.get(0).get("source"));
        assertEquals(350L, result.get(0).get("count"));
        assertEquals("cv.ee", result.get(1).get("source"));
        assertEquals(280L, result.get(1).get("count"));
        assertEquals("linkedin", result.get(2).get("source"));
        assertEquals(150L, result.get(2).get("count"));

        verify(jdbcTemplate).queryForList(
                eq("SELECT source, COUNT(*) as count FROM jobs WHERE is_active = true GROUP BY source ORDER BY count DESC"));
    }

    @Test
    void getJobsBySource_noJobs_returnsEmptyList() {
        // Arrange
        when(jdbcTemplate.queryForList(anyString())).thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = statsService.getJobsBySource();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(jdbcTemplate).queryForList(anyString());
    }

    @Test
    void getJobsBySource_singleSource_returnsSingleEntry() {
        // Arrange
        List<Map<String, Object>> mockResult = List.of(
                Map.of("source", "cvkeskus", "count", 500L)
        );

        when(jdbcTemplate.queryForList(anyString())).thenReturn(mockResult);

        // Act
        List<Map<String, Object>> result = statsService.getJobsBySource();

        // Assert
        assertEquals(1, result.size());
        assertEquals("cvkeskus", result.get(0).get("source"));
        assertEquals(500L, result.get(0).get("count"));
    }

    // --- getDailyJobTrends ---

    @Test
    void getDailyJobTrends_returnsTrendData() {
        // Arrange
        int days = 7;
        List<Map<String, Object>> mockResult = List.of(
                Map.of("date", LocalDate.of(2026, 3, 5), "count", 15L),
                Map.of("date", LocalDate.of(2026, 3, 6), "count", 22L),
                Map.of("date", LocalDate.of(2026, 3, 7), "count", 18L),
                Map.of("date", LocalDate.of(2026, 3, 8), "count", 10L),
                Map.of("date", LocalDate.of(2026, 3, 9), "count", 25L),
                Map.of("date", LocalDate.of(2026, 3, 10), "count", 30L),
                Map.of("date", LocalDate.of(2026, 3, 11), "count", 12L)
        );

        when(jdbcTemplate.queryForList(anyString(), eq(days))).thenReturn(mockResult);

        // Act
        List<Map<String, Object>> result = statsService.getDailyJobTrends(days);

        // Assert
        assertNotNull(result);
        assertEquals(7, result.size());
        assertEquals(LocalDate.of(2026, 3, 5), result.get(0).get("date"));
        assertEquals(15L, result.get(0).get("count"));
        assertEquals(LocalDate.of(2026, 3, 11), result.get(6).get("date"));
        assertEquals(12L, result.get(6).get("count"));

        verify(jdbcTemplate).queryForList(anyString(), eq(days));
    }

    @Test
    void getDailyJobTrends_emptyResult_returnsEmptyList() {
        // Arrange
        int days = 30;
        when(jdbcTemplate.queryForList(anyString(), eq(days)))
                .thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = statsService.getDailyJobTrends(days);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(jdbcTemplate).queryForList(anyString(), eq(days));
    }

    @Test
    void getDailyJobTrends_passesCorrectDaysParam() {
        // Arrange
        int days = 14;
        when(jdbcTemplate.queryForList(anyString(), eq(days)))
                .thenReturn(Collections.emptyList());

        // Act
        statsService.getDailyJobTrends(days);

        // Assert
        verify(jdbcTemplate).queryForList(
                eq("SELECT date_scraped as date, COUNT(*) as count FROM jobs " +
                   "WHERE date_scraped >= CURRENT_DATE - ? AND is_active = true " +
                   "GROUP BY date_scraped ORDER BY date_scraped"),
                eq(days));
    }
}

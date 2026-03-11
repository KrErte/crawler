package ee.itjobs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getTopSkills(int limit) {
        return jdbcTemplate.queryForList(
            "SELECT skill, COUNT(*) as count FROM jobs, jsonb_array_elements_text(skills) AS skill " +
            "WHERE is_active = true GROUP BY skill ORDER BY count DESC LIMIT ?", limit);
    }

    public List<Map<String, Object>> getJobsBySource() {
        return jdbcTemplate.queryForList(
            "SELECT source, COUNT(*) as count FROM jobs WHERE is_active = true GROUP BY source ORDER BY count DESC");
    }

    public List<Map<String, Object>> getDailyJobTrends(int days) {
        return jdbcTemplate.queryForList(
            "SELECT date_scraped as date, COUNT(*) as count FROM jobs " +
            "WHERE date_scraped >= CURRENT_DATE - ? AND is_active = true " +
            "GROUP BY date_scraped ORDER BY date_scraped", days);
    }
}

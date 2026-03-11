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

    public Map<String, Object> getAdminOverview() {
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalJobs", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jobs", Long.class));
        overview.put("activeJobs", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jobs WHERE is_active = true", Long.class));
        overview.put("expiredJobs", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jobs WHERE is_active = false", Long.class));
        overview.put("totalUsers", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users", Long.class));
        overview.put("totalApplications", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM applications", Long.class));
        overview.put("jobsWithSalary", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM jobs WHERE is_active = true AND salary_min IS NOT NULL", Long.class));
        overview.put("avgSalaryMin", jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(salary_min), 0) FROM jobs WHERE is_active = true AND salary_min IS NOT NULL", Double.class));
        overview.put("avgSalaryMax", jdbcTemplate.queryForObject(
                "SELECT COALESCE(AVG(salary_max), 0) FROM jobs WHERE is_active = true AND salary_max IS NOT NULL", Double.class));
        return overview;
    }

    public List<Map<String, Object>> getSalaryDistribution() {
        return jdbcTemplate.queryForList(
            "SELECT " +
            "  CASE " +
            "    WHEN salary_max <= 1500 THEN '0-1500' " +
            "    WHEN salary_max <= 2500 THEN '1500-2500' " +
            "    WHEN salary_max <= 3500 THEN '2500-3500' " +
            "    WHEN salary_max <= 5000 THEN '3500-5000' " +
            "    WHEN salary_max <= 7500 THEN '5000-7500' " +
            "    ELSE '7500+' " +
            "  END as range, COUNT(*) as count " +
            "FROM jobs WHERE is_active = true AND salary_max IS NOT NULL " +
            "GROUP BY range ORDER BY MIN(salary_max)");
    }

    public List<Map<String, Object>> getTopCompanies(int limit) {
        return jdbcTemplate.queryForList(
            "SELECT company, COUNT(*) as count FROM jobs WHERE is_active = true " +
            "GROUP BY company ORDER BY count DESC LIMIT ?", limit);
    }

    public List<Map<String, Object>> getWorkplaceTypeDistribution() {
        return jdbcTemplate.queryForList(
            "SELECT workplace_type as type, COUNT(*) as count FROM jobs " +
            "WHERE is_active = true AND workplace_type IS NOT NULL " +
            "GROUP BY workplace_type ORDER BY count DESC");
    }

    public List<Map<String, Object>> getJobTypeDistribution() {
        return jdbcTemplate.queryForList(
            "SELECT job_type as type, COUNT(*) as count FROM jobs " +
            "WHERE is_active = true AND job_type IS NOT NULL " +
            "GROUP BY job_type ORDER BY count DESC");
    }
}

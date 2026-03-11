package ee.itjobs.service;

import ee.itjobs.entity.Job;
import ee.itjobs.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeduplicationService {

    private final JobRepository jobRepository;
    private final JaroWinklerSimilarity jaroWinkler = new JaroWinklerSimilarity();

    private static final Pattern COMPANY_SUFFIX = Pattern.compile(
            "\\s*(OÜ|AS|Ltd\\.?|Inc\\.?|LLC|GmbH|AB|SE|SIA|UAB|Oy)\\s*$",
            Pattern.CASE_INSENSITIVE);
    private static final double FUZZY_THRESHOLD = 0.78;

    /**
     * Upsert a job: if exact dedup_key match found, update date_scraped.
     * If fuzzy match found, also update. Otherwise insert as new.
     * Returns true if job is new.
     */
    public boolean upsertJob(Job job) {
        // Phase 1: Exact dedup_key match
        Optional<Job> exactMatch = jobRepository.findByDedupKey(job.getDedupKey());
        if (exactMatch.isPresent()) {
            Job existing = exactMatch.get();
            existing.setDateScraped(LocalDate.now());
            existing.setIsActive(true);
            jobRepository.save(existing);
            return false;
        }

        // Phase 2: Fuzzy match
        String normalizedCompany = normalizeCompany(job.getCompany());
        List<Job> candidates = jobRepository.findByIsActiveTrue();
        for (Job candidate : candidates) {
            if (companiesMatch(normalizedCompany, normalizeCompany(candidate.getCompany()))
                    && titlesSimilar(job.getTitle(), candidate.getTitle())) {
                candidate.setDateScraped(LocalDate.now());
                candidate.setIsActive(true);
                jobRepository.save(candidate);
                return false;
            }
        }

        // Extract skills before saving
        job.setSkills(extractSkills(job));

        // New job
        jobRepository.save(job);
        return true;
    }

    public static List<String> extractSkills(Job job) {
        String text = ((job.getTitle() != null ? job.getTitle() : "") + " " +
                (job.getDescriptionSnippet() != null ? job.getDescriptionSnippet() : "") + " " +
                (job.getFullDescription() != null ? job.getFullDescription() : "")).toLowerCase();
        List<String> skills = new ArrayList<>();
        Map<String, List<String>> aliases = Map.ofEntries(
                Map.entry("Java", List.of("java")),
                Map.entry("Python", List.of("python")),
                Map.entry("JavaScript", List.of("javascript", "js")),
                Map.entry("TypeScript", List.of("typescript")),
                Map.entry("C#", List.of("c#", ".net", "dotnet")),
                Map.entry("C++", List.of("c++", "cpp")),
                Map.entry("Go", List.of("golang", "go developer", "go programming")),
                Map.entry("Rust", List.of("rust")),
                Map.entry("PHP", List.of("php")),
                Map.entry("Ruby", List.of("ruby")),
                Map.entry("Kotlin", List.of("kotlin")),
                Map.entry("Swift", List.of("swift")),
                Map.entry("Scala", List.of("scala")),
                Map.entry("SQL", List.of("sql", "plsql")),
                Map.entry("React", List.of("react", "reactjs")),
                Map.entry("Angular", List.of("angular")),
                Map.entry("Vue", List.of("vue", "vuejs")),
                Map.entry("Node.js", List.of("node.js", "nodejs")),
                Map.entry("Spring", List.of("spring boot", "spring framework")),
                Map.entry("Django", List.of("django")),
                Map.entry("Docker", List.of("docker")),
                Map.entry("Kubernetes", List.of("kubernetes", "k8s")),
                Map.entry("AWS", List.of("aws", "amazon web services")),
                Map.entry("Azure", List.of("azure")),
                Map.entry("GCP", List.of("gcp", "google cloud")),
                Map.entry("Terraform", List.of("terraform")),
                Map.entry("PostgreSQL", List.of("postgresql", "postgres")),
                Map.entry("MongoDB", List.of("mongodb")),
                Map.entry("Redis", List.of("redis")),
                Map.entry("Kafka", List.of("kafka")),
                Map.entry("GraphQL", List.of("graphql")),
                Map.entry("Git", List.of("git")),
                Map.entry("Linux", List.of("linux")),
                Map.entry("CI/CD", List.of("ci/cd", "cicd"))
        );
        for (var entry : aliases.entrySet()) {
            for (String alias : entry.getValue()) {
                if (text.contains(alias)) {
                    skills.add(entry.getKey());
                    break;
                }
            }
        }
        return skills;
    }

    private String normalizeCompany(String company) {
        if (company == null) return "";
        return COMPANY_SUFFIX.matcher(company.trim()).replaceAll("").trim().toLowerCase();
    }

    private boolean companiesMatch(String a, String b) {
        if (a.equals(b)) return true;
        // Word-boundary prefix match
        if (a.startsWith(b) || b.startsWith(a)) return true;
        return false;
    }

    private boolean titlesSimilar(String a, String b) {
        if (a == null || b == null) return false;
        double similarity = jaroWinkler.apply(a.toLowerCase(), b.toLowerCase());
        return similarity >= FUZZY_THRESHOLD;
    }
}

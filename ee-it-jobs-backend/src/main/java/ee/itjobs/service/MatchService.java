package ee.itjobs.service;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.dto.match.JobMatchScoreDto;
import ee.itjobs.dto.match.MatchResultDto;
import ee.itjobs.entity.Job;
import ee.itjobs.entity.User;
import ee.itjobs.entity.UserProfile;
import ee.itjobs.mapper.JobMapper;
import ee.itjobs.repository.JobRepository;
import ee.itjobs.repository.UserProfileRepository;
import ee.itjobs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    private static final Map<String, List<String>> SKILL_ALIASES = new LinkedHashMap<>();

    static {
        SKILL_ALIASES.put("python", List.of("python"));
        SKILL_ALIASES.put("javascript", List.of("javascript", "js", "ecmascript"));
        SKILL_ALIASES.put("typescript", List.of("typescript", "ts"));
        SKILL_ALIASES.put("java", List.of("java"));
        SKILL_ALIASES.put("c#", List.of("c#", "csharp", "c sharp", ".net", "dotnet"));
        SKILL_ALIASES.put("c++", List.of("c++", "cpp"));
        SKILL_ALIASES.put("go", List.of("golang", "go language", "go developer", "go programming"));
        SKILL_ALIASES.put("rust", List.of("rust"));
        SKILL_ALIASES.put("ruby", List.of("ruby"));
        SKILL_ALIASES.put("php", List.of("php"));
        SKILL_ALIASES.put("swift", List.of("swift"));
        SKILL_ALIASES.put("kotlin", List.of("kotlin"));
        SKILL_ALIASES.put("scala", List.of("scala"));
        SKILL_ALIASES.put("r", List.of("r language", "r programming", "rstudio"));
        SKILL_ALIASES.put("sql", List.of("sql", "plsql", "pl/sql", "t-sql", "tsql"));
        SKILL_ALIASES.put("react", List.of("react", "reactjs", "react.js"));
        SKILL_ALIASES.put("angular", List.of("angular", "angularjs"));
        SKILL_ALIASES.put("vue", List.of("vue", "vuejs", "vue.js"));
        SKILL_ALIASES.put("next.js", List.of("next.js", "nextjs", "next"));
        SKILL_ALIASES.put("node.js", List.of("node.js", "nodejs", "node"));
        SKILL_ALIASES.put("spring", List.of("spring", "spring boot", "spring framework", "springboot"));
        SKILL_ALIASES.put("django", List.of("django"));
        SKILL_ALIASES.put("flask", List.of("flask"));
        SKILL_ALIASES.put("express", List.of("express", "expressjs", "express.js"));
        SKILL_ALIASES.put("docker", List.of("docker", "containerization"));
        SKILL_ALIASES.put("kubernetes", List.of("kubernetes", "k8s"));
        SKILL_ALIASES.put("aws", List.of("aws", "amazon web services"));
        SKILL_ALIASES.put("azure", List.of("azure", "microsoft azure"));
        SKILL_ALIASES.put("gcp", List.of("gcp", "google cloud"));
        SKILL_ALIASES.put("terraform", List.of("terraform"));
        SKILL_ALIASES.put("ansible", List.of("ansible"));
        SKILL_ALIASES.put("jenkins", List.of("jenkins"));
        SKILL_ALIASES.put("git", List.of("git", "github", "gitlab", "bitbucket"));
        SKILL_ALIASES.put("ci/cd", List.of("ci/cd", "cicd", "continuous integration", "continuous delivery"));
        SKILL_ALIASES.put("linux", List.of("linux", "ubuntu", "centos", "debian"));
        SKILL_ALIASES.put("postgresql", List.of("postgresql", "postgres"));
        SKILL_ALIASES.put("mysql", List.of("mysql", "mariadb"));
        SKILL_ALIASES.put("mongodb", List.of("mongodb", "mongo"));
        SKILL_ALIASES.put("redis", List.of("redis"));
        SKILL_ALIASES.put("elasticsearch", List.of("elasticsearch", "elastic search", "elk"));
        SKILL_ALIASES.put("kafka", List.of("kafka", "apache kafka"));
        SKILL_ALIASES.put("rabbitmq", List.of("rabbitmq", "rabbit mq"));
        SKILL_ALIASES.put("graphql", List.of("graphql"));
        SKILL_ALIASES.put("rest api", List.of("rest api", "restful", "rest"));
        SKILL_ALIASES.put("microservices", List.of("microservices", "micro services"));
        SKILL_ALIASES.put("agile", List.of("agile", "scrum", "kanban"));
        SKILL_ALIASES.put("html", List.of("html", "html5"));
        SKILL_ALIASES.put("css", List.of("css", "css3", "sass", "scss", "less"));
        SKILL_ALIASES.put("tailwind", List.of("tailwind", "tailwindcss"));
        SKILL_ALIASES.put("figma", List.of("figma"));
        SKILL_ALIASES.put("machine learning", List.of("machine learning", "ml", "deep learning", "dl"));
        SKILL_ALIASES.put("tensorflow", List.of("tensorflow"));
        SKILL_ALIASES.put("pytorch", List.of("pytorch"));
        SKILL_ALIASES.put("data science", List.of("data science", "data analysis", "data analytics"));
        SKILL_ALIASES.put("pandas", List.of("pandas"));
        SKILL_ALIASES.put("spark", List.of("spark", "apache spark", "pyspark"));
        SKILL_ALIASES.put("hadoop", List.of("hadoop"));
        SKILL_ALIASES.put("tableau", List.of("tableau"));
        SKILL_ALIASES.put("power bi", List.of("power bi", "powerbi"));
        SKILL_ALIASES.put("jira", List.of("jira"));
        SKILL_ALIASES.put("confluence", List.of("confluence"));
        SKILL_ALIASES.put("selenium", List.of("selenium"));
        SKILL_ALIASES.put("cypress", List.of("cypress"));
        SKILL_ALIASES.put("jest", List.of("jest"));
        SKILL_ALIASES.put("junit", List.of("junit"));
        SKILL_ALIASES.put("ios", List.of("ios", "iphone", "ipad"));
        SKILL_ALIASES.put("android", List.of("android"));
        SKILL_ALIASES.put("flutter", List.of("flutter"));
        SKILL_ALIASES.put("react native", List.of("react native"));
        SKILL_ALIASES.put("unity", List.of("unity", "unity3d"));
        SKILL_ALIASES.put("unreal", List.of("unreal", "unreal engine"));
        SKILL_ALIASES.put("blockchain", List.of("blockchain", "web3", "solidity"));
        SKILL_ALIASES.put("devops", List.of("devops"));
        SKILL_ALIASES.put("sre", List.of("sre", "site reliability"));
        SKILL_ALIASES.put("security", List.of("security", "cybersecurity", "infosec", "information security"));
        SKILL_ALIASES.put("networking", List.of("networking", "tcp/ip", "dns", "load balancing"));
        SKILL_ALIASES.put("serverless", List.of("serverless", "lambda", "cloud functions"));
        SKILL_ALIASES.put("nginx", List.of("nginx"));
        SKILL_ALIASES.put("apache", List.of("apache", "httpd"));
        SKILL_ALIASES.put("svelte", List.of("svelte", "sveltekit"));
        SKILL_ALIASES.put("nuxt", List.of("nuxt", "nuxtjs", "nuxt.js"));
        SKILL_ALIASES.put("laravel", List.of("laravel"));
        SKILL_ALIASES.put("rails", List.of("rails", "ruby on rails", "ror"));
        SKILL_ALIASES.put("fastapi", List.of("fastapi", "fast api"));
        SKILL_ALIASES.put("dbt", List.of("dbt"));
        SKILL_ALIASES.put("airflow", List.of("airflow", "apache airflow"));
        SKILL_ALIASES.put("snowflake", List.of("snowflake"));
    }

    private static final Set<String> SENIOR_KEYWORDS = Set.of(
            "senior", "sr", "lead", "principal", "staff", "head", "manager",
            "director", "architect", "vp", "chief");
    private static final Set<String> JUNIOR_KEYWORDS = Set.of(
            "junior", "jr", "intern", "trainee", "entry", "graduate");

    public record CVProfile(
            String rawText,
            Set<String> skills,
            Integer yearsExperience,
            String roleLevel,
            Set<String> allKeywords
    ) {}

    public String extractPdfText(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            log.error("Failed to extract PDF text", e);
            return "";
        }
    }

    public CVProfile extractProfile(String text) {
        String lower = text.toLowerCase();
        Set<String> skills = new HashSet<>();
        Set<String> allKeywords = new HashSet<>();

        // Extract skills
        for (var entry : SKILL_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (lower.contains(alias.toLowerCase())) {
                    skills.add(entry.getKey());
                    break;
                }
            }
        }

        // Extract years of experience
        Integer yearsExperience = null;
        Pattern expPattern = Pattern.compile(
                "(\\d+)\\+?\\s*(?:years?|aasta[t]?|a\\.)\\s*(?:of\\s+)?(?:experience|kogemust?|töö)?",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = expPattern.matcher(text);
        while (matcher.find()) {
            int years = Integer.parseInt(matcher.group(1));
            if (years >= 1 && years <= 50) {
                if (yearsExperience == null || years > yearsExperience) {
                    yearsExperience = years;
                }
            }
        }

        // Detect role level — years of experience takes priority
        String roleLevel = null;
        if (yearsExperience != null) {
            if (yearsExperience >= 5) roleLevel = "senior";
            else if (yearsExperience >= 2) roleLevel = "mid";
            else roleLevel = "junior";
        }
        if (roleLevel == null) {
            for (String kw : SENIOR_KEYWORDS) {
                if (lower.contains(kw)) {
                    roleLevel = "senior";
                    break;
                }
            }
        }
        if (roleLevel == null) {
            for (String kw : JUNIOR_KEYWORDS) {
                if (lower.contains(kw)) {
                    roleLevel = "junior";
                    break;
                }
            }
        }

        // Collect all keywords
        allKeywords.addAll(skills);
        String[] words = lower.split("\\W+");
        for (String word : words) {
            if (word.length() > 2) {
                allKeywords.add(word);
            }
        }

        return new CVProfile(text, skills, yearsExperience, roleLevel, allKeywords);
    }

    public List<MatchResultDto> matchJobs(byte[] pdfBytes, int topN) {
        String text = extractPdfText(pdfBytes);
        CVProfile profile = extractProfile(text);
        return matchJobsWithProfile(profile, topN);
    }

    public List<MatchResultDto> matchJobsWithProfile(CVProfile profile, int topN) {
        List<Job> activeJobs = jobRepository.findByIsActiveTrue();
        List<MatchResultDto> results = new ArrayList<>();

        for (Job job : activeJobs) {
            if (!JobService.isItRelated(job.getTitle(), job.getDepartment())) continue;
            var result = scoreJob(profile, job);
            if (result.matchPercentage() > 0) {
                results.add(MatchResultDto.builder()
                        .job(jobMapper.toDto(job))
                        .matchPercentage(result.matchPercentage())
                        .matchedSkills(new ArrayList<>(result.matchedSkills()))
                        .build());
            }
        }

        results.sort((a, b) -> b.getMatchPercentage() - a.getMatchPercentage());
        return results.stream().limit(topN).collect(Collectors.toList());
    }

    public List<MatchResultDto> matchJobsFromProfile(String email, int topN) {
        CVProfile profile = loadProfileFromDb(email);
        return matchJobsWithProfile(profile, topN);
    }

    public List<JobMatchScoreDto> matchJobsByIds(String email, List<Long> jobIds) {
        CVProfile profile = loadProfileFromDb(email);
        List<Job> jobs = jobRepository.findAllById(jobIds);
        List<JobMatchScoreDto> results = new ArrayList<>();

        for (Job job : jobs) {
            var result = scoreJob(profile, job);
            if (result.matchPercentage() > 0) {
                results.add(JobMatchScoreDto.builder()
                        .jobId(job.getId())
                        .matchPercentage(result.matchPercentage())
                        .matchedSkills(new ArrayList<>(result.matchedSkills()))
                        .build());
            }
        }

        results.sort((a, b) -> b.getMatchPercentage() - a.getMatchPercentage());
        return results;
    }

    private CVProfile loadProfileFromDb(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        if (profile.getCvRawText() == null || profile.getCvRawText().isBlank()) {
            throw new RuntimeException("No CV uploaded in profile");
        }

        return extractProfile(profile.getCvRawText());
    }

    private record JobScore(int matchPercentage, Set<String> matchedSkills) {}

    private JobScore scoreJob(CVProfile profile, Job job) {
        String jobText = ((job.getTitle() != null ? job.getTitle() : "") + " " +
                (job.getDescriptionSnippet() != null ? job.getDescriptionSnippet() : "") + " " +
                (job.getFullDescription() != null ? job.getFullDescription() : "") + " " +
                (job.getDepartment() != null ? job.getDepartment() : "")).toLowerCase();

        // 1. Skill match (45% weight)
        Set<String> jobSkills = new HashSet<>();
        for (var entry : SKILL_ALIASES.entrySet()) {
            for (String alias : entry.getValue()) {
                if (jobText.contains(alias.toLowerCase())) {
                    jobSkills.add(entry.getKey());
                    break;
                }
            }
        }

        Set<String> matchedSkills = new HashSet<>(profile.skills());
        matchedSkills.retainAll(jobSkills);

        String titleLower = job.getTitle() != null ? job.getTitle().toLowerCase() : "";

        // Skill score: blend of coverage and absolute count
        double skillScore;
        if (jobSkills.isEmpty()) {
            // No skills detected in job listing — give baseline if it's a dev/engineer role
            boolean titleIsDevRole = titleLower.matches(".*(developer|engineer|arendaja|programmer|architect).*");
            skillScore = titleIsDevRole ? 0.4 : 0.15;
        } else {
            double skillCoverage = (double) matchedSkills.size() / jobSkills.size();
            double absBonus = Math.min(matchedSkills.size() / 3.0, 1.0);
            skillScore = skillCoverage * 0.35 + absBonus * 0.65;
        }

        // 2. Title relevance (20% weight) — match CV skills and role keywords against title
        long titleSkillHits = profile.skills().stream()
                .filter(skill -> titleLower.contains(skill))
                .count();
        // Also match role-type words (developer, engineer, etc.) for cross-language support
        long roleWordHits = 0;
        for (String roleWord : List.of("developer", "engineer", "architect", "devops",
                "full-stack", "fullstack", "backend", "frontend", "software",
                "arendaja", "insener", "tarkvara")) {
            if (titleLower.contains(roleWord) && profile.allKeywords().stream().anyMatch(kw -> kw.contains(roleWord)
                    || roleWord.contains(kw))) {
                roleWordHits++;
            }
        }
        long titleKeywordHits = profile.allKeywords().stream()
                .filter(kw -> kw.length() > 3 && titleLower.contains(kw))
                .count();
        double titleScore = Math.min((titleSkillHits * 2 + roleWordHits + titleKeywordHits) / 3.0, 1.0);

        // 3. Seniority match (10% weight) — don't penalize overqualification too harshly
        double seniorityScore = 0.5;
        boolean jobIsSenior = SENIOR_KEYWORDS.stream().anyMatch(titleLower::contains);
        boolean jobIsJunior = JUNIOR_KEYWORDS.stream().anyMatch(titleLower::contains);
        if (profile.roleLevel() != null) {
            if (profile.roleLevel().equals("senior")) {
                if (jobIsSenior) seniorityScore = 1.0;
                else if (jobIsJunior) seniorityScore = 0.4;  // overqualified but capable
                else seniorityScore = 0.7;
            } else if (profile.roleLevel().equals("mid")) {
                if (jobIsJunior) seniorityScore = 0.5;
                else if (jobIsSenior) seniorityScore = 0.5;
                else seniorityScore = 0.9;
            } else {
                if (jobIsJunior) seniorityScore = 1.0;
                else if (jobIsSenior) seniorityScore = 0.2;
                else seniorityScore = 0.6;
            }
        }

        // 4. Description keyword overlap (20% weight)
        long descKeywordHits = profile.skills().stream()
                .filter(skill -> jobText.contains(skill))
                .count();
        double descScore = Math.min(descKeywordHits / 3.0, 1.0);

        int total = (int) Math.round(skillScore * 50 + titleScore * 20 + seniorityScore * 10 + descScore * 20);
        total = Math.min(total, 100);

        return new JobScore(total, matchedSkills);
    }
}

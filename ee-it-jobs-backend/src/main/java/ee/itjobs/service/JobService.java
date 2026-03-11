package ee.itjobs.service;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.dto.job.JobFilterDto;
import ee.itjobs.entity.Job;
import ee.itjobs.enums.JobType;
import ee.itjobs.enums.WorkplaceType;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.mapper.JobMapper;
import ee.itjobs.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    private static final Set<String> IT_KEYWORDS = Set.of(
            "developer", "engineer", "programmer", "architect", "devops", "sysadmin",
            "tester", "testing", "software", "full-stack", "fullstack",
            "frontend", "front-end", "backend", "back-end",
            "machine learning", "data engineer", "data scientist",
            "scrum", "agile", "product owner", "product manager",
            "devrel", "sre",
            "arendaja", "insener", "tarkvara", "andme", "süsteemi",
            "administraator", "küber", "arvuti", "infotehnoloog",
            "helpdesk", "help desk", "infrastructure",
            "database", "automation", "platform",
            "embedded", "firmware", "robotics", "blockchain",
            "information technology", "cloud engineer", "cloud architect",
            "it support", "tech lead", "technical lead", "cto",
            "it specialist", "it manager", "it director"
    );

    public static boolean isItRelated(String title, String department) {
        if (title == null) return false;
        String lower = title.toLowerCase();
        String deptLower = department != null ? department.toLowerCase() : "";
        String combined = lower + " " + deptLower;
        for (String keyword : IT_KEYWORDS) {
            if (combined.contains(keyword)) return true;
        }
        return false;
    }

    public Page<JobDto> getJobs(String search, String company, String source,
                                 String workplaceType, String jobType,
                                 String sortBy, String sortDir,
                                 int page, int size) {
        return getJobs(search, company, source, workplaceType, jobType, null, null, null, sortBy, sortDir, page, size);
    }

    public Page<JobDto> getJobs(String search, String company, String source,
                                 String workplaceType, String jobType,
                                 List<String> skills, Integer salaryMin, Integer salaryMax,
                                 String sortBy, String sortDir,
                                 int page, int size) {
        Specification<Job> spec = Specification.where(isActive()).and(itRelevant());

        if (StringUtils.hasText(search)) {
            spec = spec.and(searchFilter(search));
        }
        if (StringUtils.hasText(company)) {
            spec = spec.and(companyFilter(company));
        }
        if (StringUtils.hasText(source)) {
            spec = spec.and(sourceFilter(source));
        }
        if (StringUtils.hasText(workplaceType)) {
            spec = spec.and(workplaceTypeFilter(workplaceType));
        }
        if (StringUtils.hasText(jobType)) {
            spec = spec.and(jobTypeFilter(jobType));
        }
        if (skills != null && !skills.isEmpty()) {
            for (String skill : skills) {
                spec = spec.and(skillFilter(skill));
            }
        }
        if (salaryMin != null) {
            spec = spec.and(salaryMinFilter(salaryMin));
        }
        if (salaryMax != null) {
            spec = spec.and(salaryMaxFilter(salaryMax));
        }

        Sort sort = Sort.by(Sort.Direction.fromString(sortDir != null ? sortDir : "DESC"),
                sortBy != null ? sortBy : "dateScraped");
        Pageable pageable = PageRequest.of(page, size, sort);

        return jobRepository.findAll(spec, pageable).map(jobMapper::toDto);
    }

    public JobDto getJob(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        return jobMapper.toDto(job);
    }

    @Transactional
    public void setJobActive(Long id, boolean active) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.setIsActive(active);
        jobRepository.save(job);
    }

    public Map<String, Object> getJobStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalJobs", jobRepository.count());
        stats.put("activeJobs", jobRepository.countByIsActiveTrue());
        stats.put("sources", jobRepository.findDistinctSources());
        return stats;
    }

    public JobFilterDto getFilters() {
        return JobFilterDto.builder()
                .companies(jobRepository.findDistinctCompanies())
                .sources(jobRepository.findDistinctSources())
                .jobTypes(Arrays.stream(JobType.values()).map(Enum::name).collect(Collectors.toList()))
                .workplaceTypes(Arrays.stream(WorkplaceType.values()).map(Enum::name).collect(Collectors.toList()))
                .build();
    }

    private Specification<Job> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    private Specification<Job> searchFilter(String search) {
        return (root, query, cb) -> {
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("company")), pattern),
                    cb.like(cb.lower(root.get("descriptionSnippet")), pattern)
            );
        };
    }

    private Specification<Job> companyFilter(String company) {
        return (root, query, cb) -> cb.equal(cb.lower(root.get("company")), company.toLowerCase());
    }

    private Specification<Job> sourceFilter(String source) {
        return (root, query, cb) -> cb.equal(root.get("source"), source);
    }

    private Specification<Job> workplaceTypeFilter(String type) {
        return (root, query, cb) -> cb.equal(root.get("workplaceType"), WorkplaceType.valueOf(type));
    }

    private Specification<Job> jobTypeFilter(String type) {
        return (root, query, cb) -> cb.equal(root.get("jobType"), JobType.valueOf(type));
    }

    private Specification<Job> skillFilter(String skill) {
        return (root, query, cb) -> {
            String sanitized = skill.replace("\"", "").replace("'", "");
            String pattern = "%\"" + sanitized + "\"%";
            return cb.like(
                    root.get("skills").as(String.class),
                    pattern);
        };
    }

    private Specification<Job> salaryMinFilter(Integer min) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("salaryMax"), min);
    }

    public List<String> getSuggestions(String query) {
        Pageable top5 = PageRequest.of(0, 5);
        List<String> companies = jobRepository.findCompanySuggestions(query, top5);
        List<String> titles = jobRepository.findTitleSuggestions(query, PageRequest.of(0, 3));
        List<String> result = new java.util.ArrayList<>(companies);
        result.addAll(titles);
        return result.stream().distinct().limit(8).collect(Collectors.toList());
    }

    private Specification<Job> salaryMaxFilter(Integer max) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("salaryMin"), max);
    }

    private Specification<Job> itRelevant() {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            for (String keyword : IT_KEYWORDS) {
                String pattern = "%" + keyword + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), pattern));
                predicates.add(cb.like(cb.lower(root.get("department")), pattern));
            }
            return cb.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}

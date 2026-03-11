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

import java.util.Arrays;
import java.util.Set;
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

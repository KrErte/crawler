package ee.itjobs.service;

import ee.itjobs.dto.job.JobDto;
import ee.itjobs.entity.Job;
import ee.itjobs.entity.SavedJob;
import ee.itjobs.entity.User;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.mapper.JobMapper;
import ee.itjobs.repository.JobRepository;
import ee.itjobs.repository.SavedJobRepository;
import ee.itjobs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    public List<JobDto> getSavedJobs(String email) {
        return savedJobRepository.findByUserEmailOrderBySavedAtDesc(email).stream()
                .map(s -> jobMapper.toDto(s.getJob()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveJob(String email, Long jobId) {
        if (savedJobRepository.existsByUserEmailAndJobId(email, jobId)) return;
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        savedJobRepository.save(SavedJob.builder().user(user).job(job).build());
    }

    @Transactional
    public void unsaveJob(String email, Long jobId) {
        savedJobRepository.deleteByUserEmailAndJobId(email, jobId);
    }

    public Set<Long> getSavedJobIds(String email) {
        return new HashSet<>(savedJobRepository.findJobIdsByUserEmail(email));
    }
}

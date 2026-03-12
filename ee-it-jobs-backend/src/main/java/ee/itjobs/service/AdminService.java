package ee.itjobs.service;

import ee.itjobs.dto.admin.JobUpdateRequest;
import ee.itjobs.dto.admin.UserListDto;
import ee.itjobs.entity.Job;
import ee.itjobs.entity.User;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.repository.ApplicationRepository;
import ee.itjobs.repository.JobRepository;
import ee.itjobs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;

    public Page<UserListDto> getUsers(String search, Pageable pageable) {
        Page<User> users;
        if (search != null && !search.isBlank()) {
            users = userRepository.findByEmailContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                    search, search, search, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::toUserListDto);
    }

    public UserListDto getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserListDto(user);
    }

    @Transactional
    public void toggleUserActive(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsActive(active);
        userRepository.save(user);
    }

    @Transactional
    public void toggleUserAdmin(Long userId, boolean admin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setIsAdmin(admin);
        userRepository.save(user);
    }

    @Transactional
    public void deleteJob(Long jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Job not found");
        }
        jobRepository.deleteById(jobId);
    }

    @Transactional
    public void updateJob(Long jobId, JobUpdateRequest request) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (request.getTitle() != null) job.setTitle(request.getTitle());
        if (request.getCompany() != null) job.setCompany(request.getCompany());
        if (request.getDescription() != null) job.setFullDescription(request.getDescription());
        if (request.getSkills() != null) job.setSkills(request.getSkills());
        if (request.getIsActive() != null) job.setIsActive(request.getIsActive());
        jobRepository.save(job);
    }

    private UserListDto toUserListDto(User user) {
        long appCount = applicationRepository.countByUserId(user.getId());
        return UserListDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .isActive(user.getIsActive())
                .isAdmin(user.getIsAdmin())
                .createdAt(user.getCreatedAt())
                .applicationCount(appCount)
                .build();
    }
}

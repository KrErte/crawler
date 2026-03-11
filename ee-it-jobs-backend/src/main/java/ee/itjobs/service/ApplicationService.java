package ee.itjobs.service;

import ee.itjobs.dto.application.*;
import ee.itjobs.entity.Application;
import ee.itjobs.entity.Job;
import ee.itjobs.entity.User;
import ee.itjobs.enums.ApplicationStatus;
import ee.itjobs.exception.DuplicateResourceException;
import ee.itjobs.exception.ResourceNotFoundException;
import ee.itjobs.mapper.ApplicationMapper;
import ee.itjobs.repository.ApplicationRepository;
import ee.itjobs.repository.JobRepository;
import ee.itjobs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationMapper applicationMapper;

    public List<ApplicationDto> getApplications(String email, ApplicationStatus status) {
        User user = findUserByEmail(email);
        List<Application> apps;
        if (status != null) {
            apps = applicationRepository.findByUserIdAndStatusOrderByAppliedAtDesc(user.getId(), status);
        } else {
            apps = applicationRepository.findByUserIdOrderByAppliedAtDesc(user.getId());
        }
        return applicationMapper.toDtoList(apps);
    }

    @Transactional
    public ApplicationDto createApplication(String email, CreateApplicationRequest request) {
        User user = findUserByEmail(email);
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (applicationRepository.existsByUserIdAndJobId(user.getId(), job.getId())) {
            throw new DuplicateResourceException("Already applied to this job");
        }
        Application app = Application.builder()
                .user(user)
                .job(job)
                .notes(request.getNotes())
                .build();
        app = applicationRepository.save(app);
        return applicationMapper.toDto(app);
    }

    @Transactional
    public ApplicationDto updateApplication(String email, Long applicationId, UpdateApplicationRequest request) {
        User user = findUserByEmail(email);
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (!app.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Application not found");
        }
        if (request.getStatus() != null) {
            app.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            app.setNotes(request.getNotes());
        }
        app = applicationRepository.save(app);
        return applicationMapper.toDto(app);
    }

    @Transactional
    public void deleteApplication(String email, Long applicationId) {
        User user = findUserByEmail(email);
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        if (!app.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Application not found");
        }
        applicationRepository.delete(app);
    }

    public boolean existsForJob(String email, Long jobId) {
        User user = findUserByEmail(email);
        return applicationRepository.existsByUserIdAndJobId(user.getId(), jobId);
    }

    public byte[] exportToCsv(String email) {
        List<ApplicationDto> apps = getApplications(email, null);
        StringBuilder sb = new StringBuilder();
        sb.append("Job Title,Company,Source,Status,Applied At,Notes,Job URL\n");
        for (ApplicationDto app : apps) {
            sb.append(escapeCsv(app.getJobTitle())).append(",");
            sb.append(escapeCsv(app.getCompany())).append(",");
            sb.append(escapeCsv(app.getSource())).append(",");
            sb.append(escapeCsv(app.getStatus().name())).append(",");
            sb.append(escapeCsv(app.getAppliedAt() != null ? app.getAppliedAt().toString() : "")).append(",");
            sb.append(escapeCsv(app.getNotes() != null ? app.getNotes() : "")).append(",");
            sb.append(escapeCsv(app.getJobUrl())).append("\n");
        }
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportToPdf(String email) throws IOException {
        List<ApplicationDto> apps = getApplications(email, null);
        try (PDDocument document = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            int appsPerPage = 25;
            for (int i = 0; i < apps.size(); i += appsPerPage) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    cs.setFont(fontBold, 16);
                    cs.beginText();
                    cs.newLineAtOffset(50, 750);
                    cs.showText("My Applications");
                    cs.endText();

                    float y = 720;
                    cs.setFont(fontBold, 9);
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText("Job Title");
                    cs.newLineAtOffset(200, 0);
                    cs.showText("Company");
                    cs.newLineAtOffset(120, 0);
                    cs.showText("Status");
                    cs.newLineAtOffset(80, 0);
                    cs.showText("Applied");
                    cs.endText();
                    y -= 15;

                    cs.setFont(font, 8);
                    int end = Math.min(i + appsPerPage, apps.size());
                    for (int j = i; j < end; j++) {
                        ApplicationDto app = apps.get(j);
                        cs.beginText();
                        cs.newLineAtOffset(50, y);
                        cs.showText(truncate(app.getJobTitle(), 35));
                        cs.newLineAtOffset(200, 0);
                        cs.showText(truncate(app.getCompany(), 20));
                        cs.newLineAtOffset(120, 0);
                        cs.showText(app.getStatus().name());
                        cs.newLineAtOffset(80, 0);
                        cs.showText(app.getAppliedAt() != null ? app.getAppliedAt().toLocalDate().toString() : "");
                        cs.endText();
                        y -= 14;
                    }
                }
            }
            if (apps.isEmpty()) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    cs.setFont(font, 12);
                    cs.beginText();
                    cs.newLineAtOffset(50, 750);
                    cs.showText("No applications found.");
                    cs.endText();
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}

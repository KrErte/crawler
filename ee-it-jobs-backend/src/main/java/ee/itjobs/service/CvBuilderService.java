package ee.itjobs.service;

import ee.itjobs.dto.profile.CvBuilderRequest;
import ee.itjobs.dto.profile.ProfileDto;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class CvBuilderService {

    private final ProfileService profileService;

    public byte[] generateCv(CvBuilderRequest request) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float y = 750;

                // Name
                cs.setFont(fontBold, 20);
                cs.beginText();
                cs.newLineAtOffset(50, y);
                cs.showText(safe(request.getFullName()));
                cs.endText();
                y -= 25;

                // Contact info
                cs.setFont(font, 10);
                StringBuilder contact = new StringBuilder();
                if (request.getEmail() != null) contact.append(request.getEmail());
                if (request.getPhone() != null) contact.append(" | ").append(request.getPhone());
                if (request.getLinkedinUrl() != null) contact.append(" | ").append(request.getLinkedinUrl());
                cs.beginText();
                cs.newLineAtOffset(50, y);
                cs.showText(contact.toString());
                cs.endText();
                y -= 20;

                // Summary
                if (request.getSummary() != null && !request.getSummary().isBlank()) {
                    y -= 10;
                    cs.setFont(fontBold, 12);
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText("PROFESSIONAL SUMMARY");
                    cs.endText();
                    y -= 5;
                    drawLine(cs, 50, y, 550, y);
                    y -= 15;
                    cs.setFont(font, 10);
                    y = drawWrappedText(cs, font, 10, request.getSummary(), 50, y, 500);
                }

                // Experience
                if (request.getExperience() != null && !request.getExperience().isEmpty()) {
                    y -= 15;
                    cs.setFont(fontBold, 12);
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText("EXPERIENCE");
                    cs.endText();
                    y -= 5;
                    drawLine(cs, 50, y, 550, y);
                    y -= 15;

                    for (CvBuilderRequest.ExperienceEntry exp : request.getExperience()) {
                        cs.setFont(fontBold, 11);
                        cs.beginText();
                        cs.newLineAtOffset(50, y);
                        cs.showText(safe(exp.getRole()));
                        cs.endText();

                        cs.setFont(font, 10);
                        String dates = safe(exp.getStartDate()) + " - " + (exp.getEndDate() != null && !exp.getEndDate().isBlank() ? exp.getEndDate() : "Present");
                        float dateWidth = font.getStringWidth(dates) / 1000 * 10;
                        cs.beginText();
                        cs.newLineAtOffset(550 - dateWidth, y);
                        cs.showText(dates);
                        cs.endText();
                        y -= 15;

                        cs.beginText();
                        cs.newLineAtOffset(50, y);
                        cs.showText(safe(exp.getCompany()));
                        cs.endText();
                        y -= 15;

                        if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                            y = drawWrappedText(cs, font, 10, exp.getDescription(), 50, y, 500);
                        }
                        y -= 10;
                    }
                }

                // Education
                if (request.getEducation() != null && !request.getEducation().isEmpty()) {
                    y -= 5;
                    cs.setFont(fontBold, 12);
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText("EDUCATION");
                    cs.endText();
                    y -= 5;
                    drawLine(cs, 50, y, 550, y);
                    y -= 15;

                    for (CvBuilderRequest.EducationEntry edu : request.getEducation()) {
                        cs.setFont(fontBold, 11);
                        cs.beginText();
                        cs.newLineAtOffset(50, y);
                        cs.showText(safe(edu.getDegree()) + " in " + safe(edu.getField()));
                        cs.endText();
                        y -= 15;

                        cs.setFont(font, 10);
                        cs.beginText();
                        cs.newLineAtOffset(50, y);
                        cs.showText(safe(edu.getInstitution()) + " | " + safe(edu.getStartDate()) + " - " + safe(edu.getEndDate()));
                        cs.endText();
                        y -= 20;
                    }
                }

                // Skills
                if (request.getSkills() != null && !request.getSkills().isEmpty()) {
                    y -= 5;
                    cs.setFont(fontBold, 12);
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText("SKILLS");
                    cs.endText();
                    y -= 5;
                    drawLine(cs, 50, y, 550, y);
                    y -= 15;

                    cs.setFont(font, 10);
                    cs.beginText();
                    cs.newLineAtOffset(50, y);
                    cs.showText(String.join(", ", request.getSkills()));
                    cs.endText();
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    public ProfileDto generateAndSaveCv(String email, CvBuilderRequest request) throws IOException {
        byte[] pdfData = generateCv(request);
        MultipartFile file = new InMemoryMultipartFile("file", "cv.pdf", "application/pdf", pdfData);
        return profileService.uploadCv(email, file);
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    private void drawLine(PDPageContentStream cs, float x1, float y1, float x2, float y2) throws IOException {
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private float drawWrappedText(PDPageContentStream cs, PDType1Font font, float fontSize, String text, float x, float y, float maxWidth) throws IOException {
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            String test = line.length() > 0 ? line + " " + word : word;
            float width = font.getStringWidth(test) / 1000 * fontSize;
            if (width > maxWidth && line.length() > 0) {
                cs.beginText();
                cs.newLineAtOffset(x, y);
                cs.showText(line.toString());
                cs.endText();
                y -= 14;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) {
            cs.beginText();
            cs.newLineAtOffset(x, y);
            cs.showText(line.toString());
            cs.endText();
            y -= 14;
        }
        return y;
    }

    /**
     * Simple in-memory MultipartFile implementation to avoid depending on spring-test.
     */
    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}

package ee.itjobs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${app.storage.cv-dir}")
    private String cvDir;

    private Path cvStoragePath;

    @PostConstruct
    public void init() {
        cvStoragePath = Paths.get(cvDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(cvStoragePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create CV storage directory", e);
        }
    }

    public String storeCv(MultipartFile file, Long userId) throws IOException {
        String filename = "cv_" + userId + "_" + UUID.randomUUID() + ".pdf";
        Path target = cvStoragePath.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    public byte[] loadCv(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    public void deleteCv(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }
}

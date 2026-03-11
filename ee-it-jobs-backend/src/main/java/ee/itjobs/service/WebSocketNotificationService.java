package ee.itjobs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void notifyScrapeComplete(int newJobs, int totalJobs) {
        messagingTemplate.convertAndSend("/topic/jobs", Map.of(
                "type", "SCRAPE_COMPLETE",
                "newJobs", newJobs,
                "totalJobs", totalJobs
        ));
    }

    public void notifyScrapeProgress(String source, int jobsFound) {
        messagingTemplate.convertAndSend("/topic/scrape-progress", Map.of(
                "source", source,
                "jobsFound", jobsFound
        ));
    }

    public void notifyUser(String email, String type, String title, String message) {
        messagingTemplate.convertAndSendToUser(email, "/queue/notifications", Map.of(
                "type", type,
                "title", title,
                "message", message,
                "timestamp", System.currentTimeMillis()
        ));
    }
}

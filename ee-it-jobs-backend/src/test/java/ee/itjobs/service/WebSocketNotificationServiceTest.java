package ee.itjobs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketNotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketNotificationService service;

    @Test
    void notifyScrapeProgress_sendsToCorrectTopic() {
        service.notifyScrapeProgress("cvkeskus", 15);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/scrape-progress"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertEquals("cvkeskus", payload.get("source"));
        assertEquals(15, payload.get("jobsFound"));
    }

    @Test
    void notifyScrapeComplete_sendsToJobsTopic() {
        service.notifyScrapeComplete(25, 100);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/jobs"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertEquals("SCRAPE_COMPLETE", payload.get("type"));
        assertEquals(25, payload.get("newJobs"));
        assertEquals(100, payload.get("totalJobs"));
    }

    @Test
    void notifyUser_sendsToUserQueue() {
        service.notifyUser("user@test.com", "JOB_ALERT", "New Jobs", "5 new jobs match your profile");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq("user@test.com"), eq("/queue/notifications"), captor.capture());

        Map<String, Object> payload = captor.getValue();
        assertEquals("JOB_ALERT", payload.get("type"));
        assertEquals("New Jobs", payload.get("title"));
        assertEquals("5 new jobs match your profile", payload.get("message"));
        assertNotNull(payload.get("timestamp"));
    }
}

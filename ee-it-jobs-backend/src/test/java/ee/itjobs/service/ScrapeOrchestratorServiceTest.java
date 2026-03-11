package ee.itjobs.service;

import ee.itjobs.entity.ScrapeRun;
import ee.itjobs.repository.ScrapeRunRepository;
import ee.itjobs.scraper.ScraperRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapeOrchestratorServiceTest {

    @Mock
    private ScraperRegistry scraperRegistry;

    @Mock
    private DeduplicationService deduplicationService;

    @Mock
    private ScrapeRunRepository scrapeRunRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @InjectMocks
    private ScrapeOrchestratorService scrapeOrchestratorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scrapeOrchestratorService, "maxConcurrency", 2);
    }

    @Test
    void isRunning_initially_returnsFalse() {
        // Act
        boolean running = scrapeOrchestratorService.isRunning();

        // Assert
        assertFalse(running);
    }

    @Test
    void getLatestRun_delegatesToRepository() {
        // Arrange
        ScrapeRun expectedRun = ScrapeRun.builder()
                .id(1L)
                .startedAt(LocalDateTime.now())
                .status("completed")
                .triggeredBy("scheduler")
                .totalJobs(150)
                .totalNewJobs(25)
                .totalErrors(3)
                .build();

        when(scrapeRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.of(expectedRun));

        // Act
        Optional<ScrapeRun> result = scrapeOrchestratorService.getLatestRun();

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("completed", result.get().getStatus());
        assertEquals("scheduler", result.get().getTriggeredBy());
        assertEquals(150, result.get().getTotalJobs());
        assertEquals(25, result.get().getTotalNewJobs());

        verify(scrapeRunRepository).findTopByOrderByStartedAtDesc();
    }

    @Test
    void getLatestRun_noRuns_returnsEmpty() {
        // Arrange
        when(scrapeRunRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.empty());

        // Act
        Optional<ScrapeRun> result = scrapeOrchestratorService.getLatestRun();

        // Assert
        assertTrue(result.isEmpty());
        verify(scrapeRunRepository).findTopByOrderByStartedAtDesc();
    }

    @Test
    void triggerScrape_createsRunAndReturnsIt() {
        // Arrange
        ScrapeRun savedRun = ScrapeRun.builder()
                .id(5L)
                .startedAt(LocalDateTime.now())
                .status("running")
                .triggeredBy("admin")
                .build();

        when(scrapeRunRepository.save(any(ScrapeRun.class))).thenReturn(savedRun);
        lenient().when(scraperRegistry.getActiveScrapers()).thenReturn(Collections.emptyList());

        // Act
        ScrapeRun result = scrapeOrchestratorService.triggerScrape("admin");

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("running", result.getStatus());
        assertEquals("admin", result.getTriggeredBy());

        ArgumentCaptor<ScrapeRun> runCaptor = ArgumentCaptor.forClass(ScrapeRun.class);
        verify(scrapeRunRepository).save(runCaptor.capture());
        ScrapeRun capturedRun = runCaptor.getValue();
        assertEquals("running", capturedRun.getStatus());
        assertEquals("admin", capturedRun.getTriggeredBy());
        assertNotNull(capturedRun.getStartedAt());
    }

    @Test
    void triggerScrape_whenAlreadyRunning_throwsIllegalStateException() {
        // Arrange - set isRunning to true via reflection
        AtomicBoolean isRunning = (AtomicBoolean) ReflectionTestUtils.getField(
                scrapeOrchestratorService, "isRunning");
        isRunning.set(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scrapeOrchestratorService.triggerScrape("admin")
        );
        assertEquals("Scrape already in progress", exception.getMessage());

        // Verify repository was never called since we failed before creating a run
        verify(scrapeRunRepository, never()).save(any(ScrapeRun.class));
    }

    @Test
    void triggerScrape_savesRunWithCorrectFields() {
        // Arrange
        ScrapeRun savedRun = ScrapeRun.builder()
                .id(1L)
                .startedAt(LocalDateTime.now())
                .status("running")
                .triggeredBy("test-user")
                .build();

        when(scrapeRunRepository.save(any(ScrapeRun.class))).thenReturn(savedRun);
        lenient().when(scraperRegistry.getActiveScrapers()).thenReturn(Collections.emptyList());

        // Act
        scrapeOrchestratorService.triggerScrape("test-user");

        // Assert
        ArgumentCaptor<ScrapeRun> captor = ArgumentCaptor.forClass(ScrapeRun.class);
        verify(scrapeRunRepository).save(captor.capture());

        ScrapeRun captured = captor.getValue();
        assertEquals("running", captured.getStatus());
        assertEquals("test-user", captured.getTriggeredBy());
        assertNotNull(captured.getStartedAt());
    }

    @Test
    void triggerScrape_secondCallAfterFirstCompletes_succeeds() throws Exception {
        // Arrange
        ScrapeRun firstRun = ScrapeRun.builder()
                .id(1L).status("running").triggeredBy("first")
                .startedAt(LocalDateTime.now()).build();
        ScrapeRun secondRun = ScrapeRun.builder()
                .id(2L).status("running").triggeredBy("second")
                .startedAt(LocalDateTime.now()).build();

        lenient().when(scrapeRunRepository.save(any(ScrapeRun.class)))
                .thenReturn(firstRun)
                .thenReturn(firstRun)  // for the completed save in async
                .thenReturn(secondRun);
        lenient().when(scraperRegistry.getActiveScrapers()).thenReturn(Collections.emptyList());

        // Act - first call
        scrapeOrchestratorService.triggerScrape("first");

        // Wait briefly for async completion to reset isRunning
        Thread.sleep(500);

        // Reset isRunning manually to simulate completion
        AtomicBoolean isRunning = (AtomicBoolean) ReflectionTestUtils.getField(
                scrapeOrchestratorService, "isRunning");
        isRunning.set(false);

        // Act - second call should succeed
        ScrapeRun result = scrapeOrchestratorService.triggerScrape("second");

        // Assert
        assertNotNull(result);
    }
}

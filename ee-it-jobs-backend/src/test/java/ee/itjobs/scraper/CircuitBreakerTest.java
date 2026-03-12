package ee.itjobs.scraper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    private CircuitBreaker cb;

    @BeforeEach
    void setUp() {
        cb = new CircuitBreaker("test", 3, 100); // 100ms timeout for fast tests
    }

    @Test
    void initialState_isClosed() {
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.allowRequest());
    }

    @Test
    void belowThreshold_staysClosed() {
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.allowRequest());
    }

    @Test
    void atThreshold_opensCircuit() {
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();

        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
        assertFalse(cb.allowRequest());
    }

    @Test
    void openCircuit_rejectsRequests() {
        // Trip the breaker
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();

        assertFalse(cb.allowRequest());
    }

    @Test
    void openCircuit_transitionsToHalfOpenAfterTimeout() throws InterruptedException {
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();

        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        // Wait for reset timeout
        Thread.sleep(150);

        assertTrue(cb.allowRequest());
        assertEquals(CircuitBreaker.State.HALF_OPEN, cb.getState());
    }

    @Test
    void halfOpen_successCloses() throws InterruptedException {
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();

        Thread.sleep(150);
        cb.allowRequest(); // Transitions to HALF_OPEN

        cb.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
        assertTrue(cb.allowRequest());
    }

    @Test
    void halfOpen_failureReopens() throws InterruptedException {
        cb.recordFailure();
        cb.recordFailure();
        cb.recordFailure();

        Thread.sleep(150);
        cb.allowRequest(); // Transitions to HALF_OPEN

        cb.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }

    @Test
    void success_resetsFailureCount() {
        cb.recordFailure();
        cb.recordFailure();
        cb.recordSuccess(); // Reset

        // Need 3 more failures to open
        cb.recordFailure();
        cb.recordFailure();
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());
    }

    @Test
    void getName_returnsName() {
        assertEquals("test", cb.getName());
    }
}

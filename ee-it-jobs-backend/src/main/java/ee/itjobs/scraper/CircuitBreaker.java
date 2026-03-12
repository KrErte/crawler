package ee.itjobs.scraper;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class CircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final String name;
    private final int failureThreshold;
    private final long resetTimeoutMs;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile Instant lastFailureTime = Instant.MIN;

    public CircuitBreaker(String name, int failureThreshold, long resetTimeoutMs) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.resetTimeoutMs = resetTimeoutMs;
    }

    public CircuitBreaker(String name) {
        this(name, 3, 60_000);
    }

    public boolean allowRequest() {
        State current = state.get();
        if (current == State.CLOSED) {
            return true;
        }
        if (current == State.OPEN) {
            if (Instant.now().toEpochMilli() - lastFailureTime.toEpochMilli() > resetTimeoutMs) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    log.info("[CircuitBreaker:{}] Transitioning from OPEN to HALF_OPEN", name);
                }
                return true;
            }
            return false;
        }
        // HALF_OPEN - allow one request to test
        return true;
    }

    public void recordSuccess() {
        if (state.get() == State.HALF_OPEN) {
            log.info("[CircuitBreaker:{}] Success in HALF_OPEN, transitioning to CLOSED", name);
            state.set(State.CLOSED);
            failureCount.set(0);
        } else if (state.get() == State.CLOSED) {
            failureCount.set(0);
        }
    }

    public void recordFailure() {
        lastFailureTime = Instant.now();
        int count = failureCount.incrementAndGet();
        if (count >= failureThreshold && state.get() != State.OPEN) {
            state.set(State.OPEN);
            log.warn("[CircuitBreaker:{}] Failure threshold reached ({}), transitioning to OPEN", name, count);
        } else if (state.get() == State.HALF_OPEN) {
            state.set(State.OPEN);
            log.warn("[CircuitBreaker:{}] Failed in HALF_OPEN, back to OPEN", name);
        }
    }

    public State getState() {
        return state.get();
    }

    public String getName() {
        return name;
    }
}

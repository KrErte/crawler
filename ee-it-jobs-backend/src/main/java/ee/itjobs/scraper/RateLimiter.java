package ee.itjobs.scraper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RateLimiter {

    private final long minIntervalMs;
    private long lastRequestTime = 0;

    public RateLimiter(double requestsPerSecond) {
        this.minIntervalMs = (long) (1000.0 / requestsPerSecond);
    }

    public synchronized void acquire() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;
        if (elapsed < minIntervalMs) {
            try {
                Thread.sleep(minIntervalMs - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestTime = System.currentTimeMillis();
    }
}

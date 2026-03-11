package ee.itjobs.config;

import ee.itjobs.security.JwtTokenProvider;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> anonBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        Bucket bucket;
        long limit;

        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            // Login/register: 5 req/min per IP
            limit = 5;
            bucket = loginBuckets.computeIfAbsent(ip, k -> Bucket.builder()
                    .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                    .build());
        } else {
            String userEmail = extractUserFromToken(request);
            if (userEmail != null) {
                // Authenticated: 200 req/min per user
                limit = 200;
                bucket = userBuckets.computeIfAbsent(userEmail, k -> Bucket.builder()
                        .addLimit(Bandwidth.classic(200, Refill.greedy(200, Duration.ofMinutes(1))))
                        .build());
            } else {
                // Anonymous: 60 req/min per IP
                limit = 60;
                bucket = anonBuckets.computeIfAbsent(ip, k -> Bucket.builder()
                        .addLimit(Bandwidth.classic(60, Refill.greedy(60, Duration.ofMinutes(1))))
                        .build());
            }
        }

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));

        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
        } else {
            long waitSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;
            response.setHeader("Retry-After", String.valueOf(waitSeconds));
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests. Please try again in " + waitSeconds + " seconds.\"}");
        }
    }

    private String extractUserFromToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtTokenProvider.validateToken(token)) {
                    return jwtTokenProvider.getEmailFromToken(token);
                }
            } catch (Exception ignored) {
                // Invalid token — treat as anonymous
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

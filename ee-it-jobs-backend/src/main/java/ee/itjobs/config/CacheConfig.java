package ee.itjobs.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        var manager = new org.springframework.cache.support.SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache("stats-overview", 100, 10),
                buildCache("stats-skills", 100, 10),
                buildCache("stats-sources", 100, 10),
                buildCache("stats-trends", 100, 10),
                buildCache("stats-salary-dist", 100, 10),
                buildCache("stats-top-companies", 100, 10),
                buildCache("stats-workplace", 100, 10),
                buildCache("stats-jobtype", 100, 10),
                buildCache("job-filters", 100, 10),
                buildCache("job-search", 500, 5),
                buildCache("job-by-id", 1000, 15),
                buildCache("recommendations", 200, 10),
                buildCache("job-translations", 500, 60)
        ));
        return manager;
    }

    private org.springframework.cache.caffeine.CaffeineCache buildCache(
            String name, int maxSize, int ttlMinutes) {
        return new org.springframework.cache.caffeine.CaffeineCache(name,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                        .build());
    }
}

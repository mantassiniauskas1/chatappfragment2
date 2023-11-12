package com.jasonpyau.chatapp.service;

import java.time.Duration;

import org.springframework.stereotype.Service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jasonpyau.chatapp.entity.User;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Service
public class RateLimitService {

    public enum Token {
        CHEAP_TOKEN(1), DEFAULT_TOKEN(2), BIG_TOKEN(5), LARGE_TOKEN(15), EXPENSIVE_TOKEN(40);

        @Getter
        private final int value;

        Token(int value) {
            this.value = value;
        }
    }

    private final int tokensPerInterval;
    private final Duration intervalDuration;
    private final Cache<User, Bucket> cache;

    @Builder(access = AccessLevel.PRIVATE)
    private RateLimitService(int tokensPerInterval, Duration intervalDuration, int maximumCacheSize, Duration cacheDuration) {
        this.tokensPerInterval = tokensPerInterval;
        this.intervalDuration = intervalDuration;
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(maximumCacheSize)
            .expireAfterAccess(cacheDuration)
            .build();
    }

    private Bandwidth getBandwidthLimit() {
        return Bandwidth.classic(tokensPerInterval, Refill.intervally(tokensPerInterval, intervalDuration));
    }

    private Bucket newBucket() {
        return Bucket.builder()
                    .addLimit(getBandwidthLimit())
                    .build();
    }

    public ConsumptionProbe rateLimit(User user, Token token) {
        Bucket bucket = cache.get(user, this::newBucket);
        return bucket.tryConsumeAndReturnRemaining(token.getValue());
    }
}

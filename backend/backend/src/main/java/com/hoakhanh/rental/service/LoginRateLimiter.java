package com.hoakhanh.rental.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class LoginRateLimiter {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private static final class Attempt {

        private int failures;
        private Instant windowStart;

        private Attempt(Instant windowStart) {
            this.failures = 0;
            this.windowStart = windowStart;
        }
    }

    private final Map<String, Attempt> attempts =
            new ConcurrentHashMap<>();

    public void assertNotBlocked(String email) {
        if (email == null) {
            return;
        }

        Attempt attempt = attempts.get(email);

        if (attempt == null) {
            return;
        }

        Instant now = Instant.now();

        if (Duration.between(attempt.windowStart, now)
                .compareTo(WINDOW) >= 0) {
            attempts.remove(email, attempt);
            return;
        }

        if (attempt.failures >= MAX_FAILED_ATTEMPTS) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts, "
                            + "please try again later");
        }
    }

    public void recordFailure(String email) {
        if (email == null) {
            return;
        }

        Instant now = Instant.now();

        attempts.compute(email, (key, old) -> {
            if (old == null
                    || Duration.between(old.windowStart, now)
                            .compareTo(WINDOW) >= 0) {
                Attempt fresh = new Attempt(now);
                fresh.failures = 1;
                return fresh;
            }

            old.failures++;
            return old;
        });

        cleanup(now);
    }

    public void recordSuccess(String email) {
        if (email == null) {
            return;
        }

        attempts.remove(email);
    }

    private void cleanup(Instant now) {
        attempts.entrySet().removeIf(entry ->
                Duration.between(
                                entry.getValue().windowStart, now)
                        .compareTo(WINDOW) >= 0);
    }
}

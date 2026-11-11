package com.hoakhanh.rental.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private final LoginRateLimiter limiter = new LoginRateLimiter();

    @Test
    void assertNotBlocked_afterFiveFailures_throwsTooManyRequests() {
        String email = "student@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(email);
        }

        assertThatThrownBy(() -> limiter.assertNotBlocked(email))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(
                        ((ResponseStatusException) ex)
                                .getStatusCode().value())
                        .isEqualTo(
                                HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    @Test
    void recordSuccess_afterFailures_clearsFailureCount() {
        String email = "student@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(email);
        }

        limiter.recordSuccess(email);

        assertThatCode(() -> limiter.assertNotBlocked(email))
                .doesNotThrowAnyException();
    }

    @Test
    void differentEmails_areIndependent() {
        String firstEmail = "first@example.com";
        String secondEmail = "second@example.com";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(firstEmail);
        }

        assertThatThrownBy(() -> limiter.assertNotBlocked(firstEmail))
                .isInstanceOf(ResponseStatusException.class);

        assertThatCode(() -> limiter.assertNotBlocked(secondEmail))
                .doesNotThrowAnyException();
    }

    @Test
    void unknownEmail_isNotBlocked() {
        assertThatCode(
                () -> limiter.assertNotBlocked("new@example.com"))
                .doesNotThrowAnyException();
    }
}

package com.hoakhanh.rental.service;

import com.hoakhanh.rental.dto.LoginRequest;
import com.hoakhanh.rental.dto.LoginResponse;
import com.hoakhanh.rental.entity.Role;
import com.hoakhanh.rental.entity.User;
import com.hoakhanh.rental.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private final LoginRateLimiter loginRateLimiter =
            new LoginRateLimiter();

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                loginRateLimiter);
    }

    private User user(boolean active) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", 1L);
        user.setFullName("Student Test");
        user.setEmail("student@example.com");
        user.setPasswordHash("hashed-password");
        user.setRole(Role.STUDENT);
        user.setActive(active);
        return user;
    }

    private static void assertStatus(
            ResponseStatusException ex,
            HttpStatus expected
    ) {
        assertThat(ex.getStatusCode().value())
                .isEqualTo(expected.value());
    }

    @Test
    void login_emailNotFound_throwsUnauthorized() {
        when(userRepository.findByEmailIgnoreCase(
                "student@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                        new LoginRequest(
                                "student@example.com",
                                "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertStatus(
                        (ResponseStatusException) ex,
                        HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_inactiveAccount_throwsUnauthorized() {
        when(userRepository.findByEmailIgnoreCase(
                "student@example.com"))
                .thenReturn(Optional.of(user(false)));

        assertThatThrownBy(() -> authService.login(
                        new LoginRequest(
                                "student@example.com",
                                "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertStatus(
                        (ResponseStatusException) ex,
                        HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmailIgnoreCase(
                "student@example.com"))
                .thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches(
                "password123", "hashed-password"))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.login(
                        new LoginRequest(
                                "student@example.com",
                                "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertStatus(
                        (ResponseStatusException) ex,
                        HttpStatus.UNAUTHORIZED));
    }

    @Test
    void login_correctPassword_returnsLoginResponseAndClearsLimiter() {
        when(userRepository.findByEmailIgnoreCase(
                "student@example.com"))
                .thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches(
                "password123", "hashed-password"))
                .thenReturn(true);
        when(jwtService.generateToken(any(User.class)))
                .thenReturn("mock-jwt-token");

        // Seed 4 failures: one more short of the block threshold.
        for (int i = 0; i < 4; i++) {
            loginRateLimiter.recordFailure("student@example.com");
        }

        LoginResponse response = authService.login(
                new LoginRequest(
                        "student@example.com", "password123"));

        assertThat(response.accessToken())
                .isEqualTo("mock-jwt-token");
        assertThat(response.email())
                .isEqualTo("student@example.com");

        // If the success had not cleared the counter, these 4 new
        // failures would add up to 8 and block the email.
        for (int i = 0; i < 4; i++) {
            loginRateLimiter.recordFailure("student@example.com");
        }

        assertThatCode(() -> loginRateLimiter.assertNotBlocked(
                "student@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void login_fiveWrongPasswords_thenSixthAttemptWithCorrectPassword_throwsTooManyRequests() {
        when(userRepository.findByEmailIgnoreCase(
                "student@example.com"))
                .thenReturn(Optional.of(user(true)));
        when(passwordEncoder.matches(anyString(), anyString()))
                .thenReturn(false);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(
                            new LoginRequest(
                                    "student@example.com",
                                    "wrong-password")))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertStatus(
                            (ResponseStatusException) ex,
                            HttpStatus.UNAUTHORIZED));
        }

        // The sixth attempt is rejected by the limiter before the
        // password is ever checked.
        assertThatThrownBy(() -> authService.login(
                        new LoginRequest(
                                "student@example.com",
                                "password123")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertStatus(
                        (ResponseStatusException) ex,
                        HttpStatus.TOO_MANY_REQUESTS));
    }
}

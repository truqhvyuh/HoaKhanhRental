
package com.hoakhanh.rental.service;

import com.hoakhanh.rental.dto.RegisterRequest;
import com.hoakhanh.rental.dto.RegisterResponse;
import com.hoakhanh.rental.entity.Role;
import com.hoakhanh.rental.entity.User;
import com.hoakhanh.rental.repository.UserRepository;
import com.hoakhanh.rental.dto.LoginRequest;
import com.hoakhanh.rental.dto.LoginResponse;
import com.hoakhanh.rental.dto.CurrentUserResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }


    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email da duoc su dung"
            );
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPasswordHash(
                passwordEncoder.encode(request.password())
        );
        user.setPhone(
                request.phone() == null || request.phone().isBlank()
                        ? null
                        : request.phone().trim()
        );
        user.setRole(Role.STUDENT);
        user.setActive(true);

        try {
            User saved = userRepository.saveAndFlush(user);

            return new RegisterResponse(
                    saved.getId(),
                    saved.getFullName(),
                    saved.getEmail(),
                    saved.getRole().name(),
                    "Dang ky tai khoan thanh cong"
            );
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Khong the tao tai khoan, email co the da ton tai"
            );
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Email hoac mat khau khong dung"
                ));

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Email hoac mat khau khong dung"
            );
        }

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Email hoac mat khau khong dung"
            );
        }

        String token = jwtService.generateToken(user);


        return new LoginResponse(
                token,
                "Bearer",
                jwtService.getExpirationSeconds(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Jwt jwt) {

        Long userId;

        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token subject"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User no longer exists"
                ));

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account is inactive"
            );
        }

        return new CurrentUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}



package com.hoakhanh.rental.controller;

import com.hoakhanh.rental.dto.RegisterRequest;
import com.hoakhanh.rental.dto.RegisterResponse;
import com.hoakhanh.rental.service.AuthService;

import com.hoakhanh.rental.dto.CurrentUserResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import com.hoakhanh.rental.dto.LoginRequest;
import com.hoakhanh.rental.dto.LoginResponse;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegisterResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                authService.getCurrentUser(jwt)
        );
    }

}

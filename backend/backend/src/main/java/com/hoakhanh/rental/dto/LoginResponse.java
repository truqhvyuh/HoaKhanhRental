
package com.hoakhanh.rental.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String fullName,
        String email,
        String role
) {
}

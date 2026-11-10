
package com.hoakhanh.rental.dto;

public record CurrentUserResponse(
        Long id,
        String fullName,
        String email,
        String role
) {
}

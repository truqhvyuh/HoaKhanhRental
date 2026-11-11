package com.hoakhanh.rental.dto;

import jakarta.validation.constraints.Size;

public record CreateContactRequest(

        @Size(
                max = 1000,
                message = "Message must not exceed 1000 characters"
        )
        String message

) {}

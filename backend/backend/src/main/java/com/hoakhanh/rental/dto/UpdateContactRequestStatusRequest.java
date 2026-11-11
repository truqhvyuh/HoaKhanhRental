package com.hoakhanh.rental.dto;

import com.hoakhanh.rental.entity.ContactRequestStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateContactRequestStatusRequest(

        @NotNull(message = "Status is required")
        ContactRequestStatus status

) {}

package com.hoakhanh.rental.controller;

import com.hoakhanh.rental.dto.ContactRequestResponse;
import com.hoakhanh.rental.dto.CreateContactRequest;
import com.hoakhanh.rental.dto.UpdateContactRequestStatusRequest;

import com.hoakhanh.rental.service.ContactRequestService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ContactRequestController {

    private final ContactRequestService contactRequestService;

    public ContactRequestController(
            ContactRequestService contactRequestService
    ) {
        this.contactRequestService = contactRequestService;
    }

    // Sinh vien gui yeu cau lien he chu tro
    @PostMapping("/api/v1/rooms/{roomId}/contact-requests")
    public ResponseEntity<ContactRequestResponse> createContactRequest(
            @PathVariable Long roomId,
            @Valid @RequestBody(required = false) CreateContactRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ContactRequestResponse response =
                contactRequestService.createContactRequest(
                        roomId,
                        request,
                        jwt
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Sinh vien xem cac yeu cau da gui
    @GetMapping("/api/v1/contact-requests/mine")
    public List<ContactRequestResponse> getMyContactRequests(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return contactRequestService.getMyContactRequests(jwt);
    }

    // Chu tro xem cac yeu cau lien he cua minh
    @GetMapping("/api/v1/landlord/contact-requests")
    public List<ContactRequestResponse> getLandlordContactRequests(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return contactRequestService.getLandlordContactRequests(jwt);
    }

    // Chu tro cap nhat trang thai yeu cau lien he
    @PatchMapping(
            "/api/v1/landlord/contact-requests/{contactRequestId}/status"
    )
    public ContactRequestResponse updateContactRequestStatus(
            @PathVariable Long contactRequestId,
            @Valid @RequestBody UpdateContactRequestStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return contactRequestService.updateContactRequestStatus(
                contactRequestId,
                request,
                jwt
        );
    }
}

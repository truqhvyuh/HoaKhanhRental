
package com.hoakhanh.rental.controller;

import com.hoakhanh.rental.dto.CreateRoomRequest;
import com.hoakhanh.rental.service.LandlordRoomService;
import com.hoakhanh.rental.dto.LandlordRoomResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.hoakhanh.rental.dto.UpdateRoomRequest;
import org.springframework.web.bind.annotation.PutMapping;
import com.hoakhanh.rental.dto.UpdateRoomStatusRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/landlord/rooms")
public class LandlordRoomController {

    private final LandlordRoomService landlordRoomService;

    public LandlordRoomController(
            LandlordRoomService landlordRoomService
    ) {
        this.landlordRoomService = landlordRoomService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long roomId = landlordRoomService.createRoom(
                request,
                jwt
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "id", roomId,
                        "message",
                        "Room created successfully",
                        "moderationStatus", "PENDING"
                ));
    }

    @PutMapping("/{roomId}")
    public LandlordRoomResponse updateMyRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return landlordRoomService.updateMyRoom(
                roomId, request, jwt
        );
    }


    @GetMapping("/{roomId}")
    public LandlordRoomResponse getMyRoomById(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return landlordRoomService.getMyRoomById(roomId, jwt);
    }


    @GetMapping
    public List<LandlordRoomResponse> getMyRooms(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return landlordRoomService.getMyRooms(jwt, page, size);
    }

    @PatchMapping("/{roomId}/status")
    public LandlordRoomResponse updateRoomStatus(
            @PathVariable Long roomId,
            @Valid @RequestBody UpdateRoomStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return landlordRoomService.updateRoomStatus(
                roomId, request, jwt
        );
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        landlordRoomService.deleteMyRoom(roomId, jwt);
    }


}

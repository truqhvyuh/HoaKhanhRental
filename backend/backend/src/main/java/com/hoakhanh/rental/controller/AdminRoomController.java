
package com.hoakhanh.rental.controller;

import com.hoakhanh.rental.dto.AdminRoomResponse;
import com.hoakhanh.rental.dto.RejectRoomRequest;
import com.hoakhanh.rental.service.AdminRoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/rooms")
public class AdminRoomController {

    private final AdminRoomService adminRoomService;

    public AdminRoomController(AdminRoomService adminRoomService) {
        this.adminRoomService = adminRoomService;
    }

    @GetMapping("/pending")
    public List<AdminRoomResponse> getPendingRooms(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return adminRoomService.getPendingRooms(jwt, page, size);
    }

    @PatchMapping("/{roomId}/approve")
    public AdminRoomResponse approveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return adminRoomService.approveRoom(roomId, jwt);
    }

    @PatchMapping("/{roomId}/reject")
    public AdminRoomResponse rejectRoom(
            @PathVariable Long roomId,
            @Valid @RequestBody RejectRoomRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return adminRoomService.rejectRoom(
                roomId,
                request.rejectionReason(),
                jwt
        );
    }
}

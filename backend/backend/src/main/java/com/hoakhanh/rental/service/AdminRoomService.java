
package com.hoakhanh.rental.service;

import com.hoakhanh.rental.dto.AdminRoomResponse;
import com.hoakhanh.rental.entity.*;
import com.hoakhanh.rental.repository.RoomRepository;
import com.hoakhanh.rental.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AdminRoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public AdminRoomService(
            RoomRepository roomRepository,
            UserRepository userRepository
    ) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    private void requireAdmin(Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        Long userId;

        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account is inactive"
            );
        }

        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Admin access required"
            );
        }
    }

    @Transactional(readOnly = true)
    public List<AdminRoomResponse> getPendingRooms(
            Jwt jwt,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid pagination parameters"
            );
        }

        requireAdmin(jwt);

        Page<Room> roomPage = roomRepository
                .findByModerationStatusAndDeletedAtIsNull(
                        ModerationStatus.PENDING,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Direction.ASC,
                                        "createdAt"
                                )
                        )
                );

        return roomPage.getContent()
                .stream()
                .map(AdminRoomResponse::from)
                .toList();
    }

    @Transactional
    public AdminRoomResponse approveRoom(Long roomId, Jwt jwt) {
        requireAdmin(jwt);

        Room room = getPendingRoom(roomId);
        room.setModerationStatus(ModerationStatus.APPROVED);
        room.setRejectionReason(null);

        return AdminRoomResponse.from(roomRepository.save(room));
    }

    @Transactional
    public AdminRoomResponse rejectRoom(
            Long roomId,
            String rejectionReason,
            Jwt jwt
    ) {
        requireAdmin(jwt);

        Room room = getPendingRoom(roomId);
        room.setModerationStatus(ModerationStatus.REJECTED);
        room.setRejectionReason(rejectionReason.trim());

        return AdminRoomResponse.from(roomRepository.save(room));
    }

    private Room getPendingRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found"
                ));

        if (room.getDeletedAt() != null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Room not found"
            );
        }

        if (room.getModerationStatus() != ModerationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Room is no longer pending"
            );
        }

        return room;
    }
}

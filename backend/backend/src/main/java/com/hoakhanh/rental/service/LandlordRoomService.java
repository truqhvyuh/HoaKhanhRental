
package com.hoakhanh.rental.service;

import com.hoakhanh.rental.dto.CreateRoomRequest;
import com.hoakhanh.rental.entity.Room;
import com.hoakhanh.rental.entity.User;
import com.hoakhanh.rental.entity.Role;
import com.hoakhanh.rental.entity.ModerationStatus;
import com.hoakhanh.rental.entity.OccupancyStatus;
import com.hoakhanh.rental.entity.VisibilityStatus;
import com.hoakhanh.rental.repository.RoomRepository;
import com.hoakhanh.rental.repository.UserRepository;
import com.hoakhanh.rental.dto.LandlordRoomResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.hoakhanh.rental.dto.UpdateRoomRequest;
import com.hoakhanh.rental.entity.ModerationStatus;

import org.springframework.http.HttpStatus;
import com.hoakhanh.rental.dto.UpdateRoomStatusRequest;
import java.time.OffsetDateTime;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LandlordRoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public LandlordRoomService(
            RoomRepository roomRepository,
            UserRepository userRepository
    ) {
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long createRoom(
            CreateRoomRequest request,
            Jwt jwt
    ) {
        Long userId;

        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token subject"
            );
        }

        User landlord = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User no longer exists"
                ));

        if (!landlord.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account is inactive"
            );
        }

        if (landlord.getRole() != Role.LANDLORD) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only landlords can create rooms"
            );
        }

        Room room = new Room();

        room.setLandlord(landlord);
        room.setTitle(request.title().trim());
        room.setAddress(request.address().trim());

        room.setAreaName(
                request.areaName() == null
                        ? null
                        : request.areaName().trim()
        );

        room.setDescription(
                request.description() == null
                        ? null
                        : request.description().trim()
        );

        room.setPrice(request.price());
        room.setAreaSqm(request.areaSqm());
        room.setMaxOccupants(request.maxOccupants());

        room.setModerationStatus(
                ModerationStatus.PENDING
        );

        room.setOccupancyStatus(
                OccupancyStatus.AVAILABLE
        );

        room.setVisibilityStatus(
                VisibilityStatus.VISIBLE
        );

        Room savedRoom = roomRepository.save(room);

        return savedRoom.getId();
    }

    @Transactional
    public LandlordRoomResponse updateMyRoom(
            Long roomId,
            UpdateRoomRequest request,
            Jwt jwt
    ) {
        Long userId;

        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token"
            );
        }

        User landlord = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        if (!landlord.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account is inactive"
            );
        }

        if (landlord.getRole() != Role.LANDLORD) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only landlords can edit rooms"
            );
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found"
                ));

        if (room.getDeletedAt() != null ||
                !room.getLandlord().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Room not found"
            );
        }

        room.setTitle(request.title().trim());
        room.setAddress(request.address().trim());
        room.setAreaName(request.areaName());
        room.setPrice(request.price());
        room.setAreaSqm(request.areaSqm());
        room.setMaxOccupants(request.maxOccupants());
        room.setDescription(request.description());

        // Nội dung thay đổi phải được duyệt lại.
        room.setModerationStatus(ModerationStatus.PENDING);

        Room updated = roomRepository.save(room);
        return LandlordRoomResponse.from(updated);
    }


    @Transactional(readOnly = true)
    public List<LandlordRoomResponse> getMyRooms(Jwt jwt) {
        Long userId;

        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token subject"
            );
        }

        User landlord = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User no longer exists"
                ));

        if (!landlord.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account is inactive"
            );
        }

        if (landlord.getRole() != Role.LANDLORD) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only landlords can view their rooms"
            );
        }

        return roomRepository
                .findByLandlord_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        landlord.getId()
                )
                .stream()
                .map(LandlordRoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LandlordRoomResponse getMyRoomById(
            Long roomId,
            Jwt jwt
    ) {
        Long userId;

        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid token"
            );
        }

        User landlord = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"
                ));

        if (!landlord.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Account is inactive"
            );
        }

        if (landlord.getRole() != Role.LANDLORD) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only landlords can view their rooms"
            );
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found"
                ));

        if (room.getDeletedAt() != null ||
                !room.getLandlord().getId().equals(landlord.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Room not found"
            );
        }

        return LandlordRoomResponse.from(room);
    }

    private Room findOwnedRoom(Long roomId, Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Authentication required"
            );
        }

        Long userId;

        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid token"
            );
        }

        User landlord = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"
                ));

        if (!landlord.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Account is inactive"
            );
        }

        if (landlord.getRole() != Role.LANDLORD) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Landlord access required"
            );
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Room not found"
                ));

        if (room.getDeletedAt() != null ||
                !room.getLandlord().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Room not found"
            );
        }

        return room;
    }

    @Transactional
    public LandlordRoomResponse updateRoomStatus(
            Long roomId,
            UpdateRoomStatusRequest request,
            Jwt jwt
    ) {
        Room room = findOwnedRoom(roomId, jwt);

        room.setVisibilityStatus(request.visibilityStatus());
        room.setOccupancyStatus(request.occupancyStatus());

        return LandlordRoomResponse.from(roomRepository.save(room));
    }

    @Transactional
    public void deleteMyRoom(Long roomId, Jwt jwt) {
        Room room = findOwnedRoom(roomId, jwt);

        room.setDeletedAt(OffsetDateTime.now());
        roomRepository.save(room);
    }


}

package com.hoakhanh.rental.service;

import com.hoakhanh.rental.dto.ContactRequestResponse;
import com.hoakhanh.rental.dto.CreateContactRequest;
import com.hoakhanh.rental.dto.UpdateContactRequestStatusRequest;

import com.hoakhanh.rental.entity.ContactRequest;
import com.hoakhanh.rental.entity.ContactRequestStatus;
import com.hoakhanh.rental.entity.ModerationStatus;
import com.hoakhanh.rental.entity.OccupancyStatus;
import com.hoakhanh.rental.entity.Role;
import com.hoakhanh.rental.entity.Room;
import com.hoakhanh.rental.entity.User;
import com.hoakhanh.rental.entity.VisibilityStatus;

import com.hoakhanh.rental.repository.ContactRequestRepository;
import com.hoakhanh.rental.repository.RoomRepository;
import com.hoakhanh.rental.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ContactRequestService {

    private final ContactRequestRepository contactRequestRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public ContactRequestService(
            ContactRequestRepository contactRequestRepository,
            RoomRepository roomRepository,
            UserRepository userRepository
    ) {
        this.contactRequestRepository = contactRequestRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
    }

    // Lay tai khoan hien tai tu JWT va kiem tra trang thai tai khoan
    private User requireUser(Jwt jwt) {

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

        return user;
    }

    // Kiem tra vai tro cua tai khoan
    private User requireRole(
            Jwt jwt,
            Role role,
            String message
    ) {
        User user = requireUser(jwt);

        if (user.getRole() != role) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    message
            );
        }

        return user;
    }

    // Tao yeu cau lien he chu tro
    @Transactional
    public ContactRequestResponse createContactRequest(
            Long roomId,
            CreateContactRequest request,
            Jwt jwt
    ) {
        User student = requireRole(
                jwt,
                Role.STUDENT,
                "Only students can create contact requests"
        );

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Room not found"
                ));

        // Chi cho phep lien he phong dang duoc cong khai
        if (
                room.getModerationStatus()
                        != ModerationStatus.APPROVED
                        ||
                room.getOccupancyStatus()
                        != OccupancyStatus.AVAILABLE
                        ||
                room.getVisibilityStatus()
                        != VisibilityStatus.VISIBLE
                        ||
                room.getDeletedAt() != null
        ) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Room not found"
            );
        }

        // Khong cho sinh vien lien he phong cua chinh minh
        if (student.getId().equals(room.getLandlord().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You cannot contact your own room"
            );
        }

        // Khong cho tao yeu cau PENDING trung lap
        boolean hasPendingRequest =
                contactRequestRepository
                        .existsByStudent_IdAndRoom_IdAndStatus(
                                student.getId(),
                                room.getId(),
                                ContactRequestStatus.PENDING
                        );

        if (hasPendingRequest) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A pending contact request already exists"
            );
        }

        ContactRequest contactRequest = new ContactRequest();

        contactRequest.setStudent(student);
        contactRequest.setRoom(room);

        String message = request == null ? null : request.message();

        contactRequest.setMessage(
                message == null || message.isBlank()
                        ? null
                        : message.trim()
        );

        contactRequest.setStatus(ContactRequestStatus.PENDING);

        ContactRequest savedRequest =
                contactRequestRepository.save(contactRequest);

        return ContactRequestResponse.from(savedRequest);
    }

    // Sinh vien xem cac yeu cau lien he cua minh
    @Transactional(readOnly = true)
    public List<ContactRequestResponse> getMyContactRequests(
            Jwt jwt
    ) {
        User student = requireRole(
                jwt,
                Role.STUDENT,
                "Only students can view their contact requests"
        );

        return contactRequestRepository
                .findByStudent_IdOrderByCreatedAtDesc(student.getId())
                .stream()
                .map(ContactRequestResponse::from)
                .toList();
    }

    // Chu tro xem cac yeu cau lien he thuoc cac phong cua minh
    @Transactional(readOnly = true)
    public List<ContactRequestResponse> getLandlordContactRequests(
            Jwt jwt
    ) {
        User landlord = requireRole(
                jwt,
                Role.LANDLORD,
                "Only landlords can view contact requests"
        );

        return contactRequestRepository
                .findByRoom_Landlord_IdOrderByCreatedAtDesc(
                        landlord.getId()
                )
                .stream()
                .map(ContactRequestResponse::from)
                .toList();
    }

    // Chu tro cap nhat trang thai yeu cau lien he
    @Transactional
    public ContactRequestResponse updateContactRequestStatus(
            Long contactRequestId,
            UpdateContactRequestStatusRequest request,
            Jwt jwt
    ) {
        User landlord = requireRole(
                jwt,
                Role.LANDLORD,
                "Only landlords can update contact requests"
        );

        ContactRequest contactRequest = contactRequestRepository
                .findByIdAndRoom_Landlord_Id(
                        contactRequestId,
                        landlord.getId()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Contact request not found"
                ));

        ContactRequestStatus currentStatus =
                contactRequest.getStatus();

        ContactRequestStatus newStatus = request.status();

        // Chi chap nhan cac chuyen trang thai hop le
        boolean validTransition =
                (currentStatus == ContactRequestStatus.PENDING
                        &&
                        (newStatus == ContactRequestStatus.RESPONDED
                                || newStatus == ContactRequestStatus.CLOSED))
                ||
                (currentStatus == ContactRequestStatus.RESPONDED
                        &&
                        newStatus == ContactRequestStatus.CLOSED);

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invalid contact request status transition"
            );
        }

        contactRequest.setStatus(newStatus);

        ContactRequest updatedRequest =
                contactRequestRepository.save(contactRequest);

        return ContactRequestResponse.from(updatedRequest);
    }
}

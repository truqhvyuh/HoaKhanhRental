package com.hoakhanh.rental.dto;

import com.hoakhanh.rental.entity.ContactRequest;
import com.hoakhanh.rental.entity.ContactRequestStatus;
import com.hoakhanh.rental.entity.Room;
import com.hoakhanh.rental.entity.User;

import java.time.OffsetDateTime;

public record ContactRequestResponse(

        Long id,

        Long roomId,
        String roomTitle,

        Long studentId,
        String studentName,
        String studentPhone,
        String studentEmail,

        Long landlordId,
        String landlordName,
        String landlordPhone,

        String message,
        ContactRequestStatus status,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt

) {

    public static ContactRequestResponse from(
            ContactRequest contactRequest
    ) {
        Room room = contactRequest.getRoom();
        User student = contactRequest.getStudent();
        User landlord = room.getLandlord();

        return new ContactRequestResponse(
                contactRequest.getId(),

                room.getId(),
                room.getTitle(),

                student.getId(),
                student.getFullName(),
                student.getPhone(),
                student.getEmail(),

                landlord.getId(),
                landlord.getFullName(),
                landlord.getPhone(),

                contactRequest.getMessage(),
                contactRequest.getStatus(),

                contactRequest.getCreatedAt(),
                contactRequest.getUpdatedAt()
        );
    }
}

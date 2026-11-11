
package com.hoakhanh.rental.dto;

import com.hoakhanh.rental.entity.Room;
import java.math.BigDecimal;

public record AdminRoomResponse(
        Long id,
        String title,
        String description,
        String address,
        String areaName,
        BigDecimal price,
        BigDecimal areaSqm,
        Integer maxOccupants,
        String moderationStatus,
        String rejectionReason,
        String occupancyStatus,
        String visibilityStatus,
        Long landlordId
) {
    public static AdminRoomResponse from(Room room) {
        return new AdminRoomResponse(
                room.getId(),
                room.getTitle(),
                room.getDescription(),
                room.getAddress(),
                room.getAreaName(),
                room.getPrice(),
                room.getAreaSqm(),
                room.getMaxOccupants(),
                room.getModerationStatus().name(),
                room.getRejectionReason(),
                room.getOccupancyStatus().name(),
                room.getVisibilityStatus().name(),
                room.getLandlord().getId()
        );
    }
}

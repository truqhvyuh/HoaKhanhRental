
package com.hoakhanh.rental.dto;

import com.hoakhanh.rental.entity.Room;
import java.math.BigDecimal;

public record LandlordRoomResponse(
        Long id,
        String title,
        String description,
        String address,
        String areaName,
        BigDecimal price,
        BigDecimal areaSqm,
        Integer maxOccupants,
        String moderationStatus,
        String occupancyStatus,
        String visibilityStatus
) {
    public static LandlordRoomResponse from(Room room) {
        return new LandlordRoomResponse(
                room.getId(),
                room.getTitle(),
                room.getDescription(),
                room.getAddress(),
                room.getAreaName(),
                room.getPrice(),
                room.getAreaSqm(),
                room.getMaxOccupants(),
                room.getModerationStatus().name(),
                room.getOccupancyStatus().name(),
                room.getVisibilityStatus().name()
        );
    }
}


package com.hoakhanh.rental.dto;

import com.hoakhanh.rental.entity.RoomPhoto;

public record RoomPhotoResponse(
        Long id,
        String url
) {
    public static RoomPhotoResponse from(RoomPhoto photo) {
        return new RoomPhotoResponse(
                photo.getId(),
                "/uploads/rooms/" + photo.getFilename()
        );
    }
}

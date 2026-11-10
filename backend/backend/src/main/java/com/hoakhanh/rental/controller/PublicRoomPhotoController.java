
package com.hoakhanh.rental.controller;

import com.hoakhanh.rental.dto.RoomPhotoResponse;
import com.hoakhanh.rental.service.RoomPhotoService;
import com.hoakhanh.rental.service.RoomService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
public class PublicRoomPhotoController {

    private final RoomPhotoService photoService;
    private final RoomService roomService;

    public PublicRoomPhotoController(
            RoomPhotoService photoService,
            RoomService roomService
    ) {
        this.photoService = photoService;
        this.roomService = roomService;
    }

    @GetMapping("/{roomId}/photos")
    public List<RoomPhotoResponse> getPublicPhotos(
            @PathVariable Long roomId
    ) {
        // Su dung bo loc phong cong khai da lam truoc do.
        roomService.getRoomById(roomId);

        return photoService.listPhotos(roomId);
    }
}

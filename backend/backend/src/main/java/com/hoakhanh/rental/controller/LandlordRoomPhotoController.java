
package com.hoakhanh.rental.controller;

import com.hoakhanh.rental.dto.RoomPhotoResponse;
import com.hoakhanh.rental.service.RoomPhotoService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/landlord/rooms/{roomId}/photos")
public class LandlordRoomPhotoController {

    private final RoomPhotoService photoService;

    public LandlordRoomPhotoController(RoomPhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    public List<RoomPhotoResponse> getPhotos(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return photoService.getMyPhotos(roomId, jwt);
    }

    @PostMapping(consumes = "multipart/form-data")
    public List<RoomPhotoResponse> uploadPhotos(
            @PathVariable Long roomId,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return photoService.upload(roomId, files, jwt);
    }

    @DeleteMapping("/{photoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePhoto(
            @PathVariable Long roomId,
            @PathVariable Long photoId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        photoService.deletePhoto(roomId, photoId, jwt);
    }

    @GetMapping("/{photoId}/file")
    public ResponseEntity<Resource> getMyPhotoFile(
            @PathVariable Long roomId,
            @PathVariable Long photoId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return photoService.getMyPhotoFile(
                roomId,
                photoId,
                jwt
        );
    }

}

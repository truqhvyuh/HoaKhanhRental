
package com.hoakhanh.rental.controller;

import com.hoakhanh.rental.entity.*;
import com.hoakhanh.rental.repository.RoomPhotoRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
public class PublicRoomImageController {

    private final RoomPhotoRepository photoRepository;
    private final Path uploadPath;

    public PublicRoomImageController(
            RoomPhotoRepository photoRepository,
            @Value("${app.upload-dir:uploads/rooms}")
            String uploadDir
    ) {
        this.photoRepository = photoRepository;
        this.uploadPath = Path.of(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    @GetMapping("/uploads/rooms/{filename:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String filename
    ) {
        // 1. Chi chap nhan ten anh do he thong tao.
        if (!filename.matches(
                "[a-fA-F0-9-]{36}\\.(jpg|png)"
        )) {
            throw notFound();
        }

        // 2. Tim anh trong PostgreSQL.
        RoomPhoto photo = photoRepository
                .findByFilename(filename)
                .orElseThrow(this::notFound);

        Room room = photo.getRoom();

        // 3. Chi tra anh cua phong cong khai.
        if (
                room.getDeletedAt() != null ||
                        room.getModerationStatus()
                                != ModerationStatus.APPROVED ||
                        room.getVisibilityStatus()
                                != VisibilityStatus.VISIBLE ||
                        room.getOccupancyStatus()
                                != OccupancyStatus.AVAILABLE
        ) {
            throw notFound();
        }

        // 4. Doc dung file trong thu muc upload.
        Path imagePath = uploadPath.resolve(filename)
                .normalize();

        if (!imagePath.startsWith(uploadPath)) {
            throw notFound();
        }

        if (!Files.isRegularFile(imagePath) ||
                !Files.isReadable(imagePath)) {
            throw notFound();
        }

        try {
            byte[] bytes = Files.readAllBytes(imagePath);

            MediaType mediaType = filename.endsWith(".png")
                    ? MediaType.IMAGE_PNG
                    : MediaType.IMAGE_JPEG;

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(mediaType)
                    .contentLength(bytes.length)
                    .body(new ByteArrayResource(bytes));

        } catch (IOException ex) {
            throw notFound();
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Image not found"
        );
    }
}

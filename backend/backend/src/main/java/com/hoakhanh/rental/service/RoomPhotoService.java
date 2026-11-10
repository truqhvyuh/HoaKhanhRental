
package com.hoakhanh.rental.service;

import com.hoakhanh.rental.dto.RoomPhotoResponse;
import com.hoakhanh.rental.entity.*;
import com.hoakhanh.rental.repository.RoomPhotoRepository;
import com.hoakhanh.rental.repository.RoomRepository;
import com.hoakhanh.rental.repository.UserRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.CacheControl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

@Service
public class RoomPhotoService {

    private final RoomRepository roomRepository;
    private final RoomPhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final Path uploadPath;

    public RoomPhotoService(
            RoomRepository roomRepository,
            RoomPhotoRepository photoRepository,
            UserRepository userRepository,
            @Value("${app.upload-dir:uploads/rooms}") String uploadDir
    ) {
        this.roomRepository = roomRepository;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.uploadPath = Path.of(uploadDir)
                .toAbsolutePath()
                .normalize();
    }

    private Room findOwnedRoom(Long roomId, Jwt jwt) {
        if (jwt == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Login required"
            );
        }

        Long userId;

        try {
            userId = Long.parseLong(jwt.getSubject());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid token"
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"
                ));

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Account inactive"
            );
        }

        if (user.getRole() != Role.LANDLORD) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Landlord required"
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

    @Transactional(readOnly = true)
    public List<RoomPhotoResponse> getMyPhotos(
            Long roomId, Jwt jwt
    ) {
        findOwnedRoom(roomId, jwt);
        return listPhotos(roomId);
    }

    @Transactional(readOnly = true)
    public List<RoomPhotoResponse> listPhotos(Long roomId) {
        return photoRepository.findByRoom_IdOrderByIdAsc(roomId)
                .stream()
                .map(RoomPhotoResponse::from)
                .toList();
    }

    @Transactional
    public List<RoomPhotoResponse> upload(
            Long roomId,
            List<MultipartFile> files,
            Jwt jwt
    ) {
        Room room = findOwnedRoom(roomId, jwt);

        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No files provided"
            );
        }

        long existing = photoRepository.countByRoom_Id(roomId);

        if (existing + files.size() > 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Maximum 5 photos per room"
            );
        }

        // Kiem tra tat ca file truoc khi luu.
        List<String> extensions = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty() ||
                    file.getSize() > 5L * 1024 * 1024) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Each image must be between 1 byte and 5 MB"
                );
            }

            String contentType = file.getContentType();

            if (!"image/jpeg".equals(contentType) &&
                    !"image/png".equals(contentType)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Only JPG and PNG are allowed"
                );
            }

            // Xac minh noi dung anh, khong chi tin duoi file.
            try (ImageInputStream imageStream =
                         ImageIO.createImageInputStream(
                                 file.getInputStream()
                         )) {

                if (imageStream == null) {
                    throw new IOException("Invalid image");
                }

                Iterator<ImageReader> readers =
                        ImageIO.getImageReaders(imageStream);

                if (!readers.hasNext()) {
                    throw new IOException("Unsupported image");
                }

                ImageReader reader = readers.next();

                try {
                    reader.setInput(imageStream);

                    String format = reader.getFormatName();
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);

                    if (width < 1 || height < 1 ||
                            width > 6000 || height > 6000) {
                        throw new IOException("Invalid dimensions");
                    }

                    if ("JPEG".equalsIgnoreCase(format) &&
                            "image/jpeg".equals(contentType)) {
                        extensions.add(".jpg");
                    } else if ("PNG".equalsIgnoreCase(format) &&
                            "image/png".equals(contentType)) {
                        extensions.add(".png");
                    } else {
                        throw new IOException("Image type mismatch");
                    }
                } finally {
                    reader.dispose();
                }
            } catch (IOException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid JPG or PNG image"
                );
            }
        }

        List<Path> savedPaths = new ArrayList<>();

        try {
            Files.createDirectories(uploadPath);

            for (int i = 0; i < files.size(); i++) {
                String filename =
                        UUID.randomUUID() + extensions.get(i);

                Path destination = uploadPath.resolve(filename);

                try (var input = files.get(i).getInputStream()) {
                    Files.copy(input, destination);
                }

                savedPaths.add(destination);

                RoomPhoto photo = new RoomPhoto();
                photo.setRoom(room);
                photo.setFilename(filename);

                photoRepository.save(photo);
            }

            photoRepository.flush();
            return listPhotos(roomId);

        } catch (IOException | RuntimeException ex) {
            // Xoa file vua tai len neu thao tac that bai.
            for (Path path : savedPaths) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            }

            if (ex instanceof ResponseStatusException statusEx) {
                throw statusEx;
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save images",
                    ex
            );
        }
    }

    @Transactional
    public void deletePhoto(
            Long roomId, Long photoId, Jwt jwt
    ) {
        findOwnedRoom(roomId, jwt);

        RoomPhoto photo = photoRepository
                .findByIdAndRoom_Id(photoId, roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Photo not found"
                ));

        Path imagePath = uploadPath.resolve(photo.getFilename());

        photoRepository.delete(photo);
        photoRepository.flush();

        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not delete image",
                    ex
            );
        }
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> getMyPhotoFile(
            Long roomId,
            Long photoId,
            Jwt jwt
    ) {
        // Kiem tra phong co thuoc chu tro dang dang nhap khong
        findOwnedRoom(roomId, jwt);

        RoomPhoto photo = photoRepository
                .findByIdAndRoom_Id(photoId, roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Photo not found"
                ));

        Path filePath = uploadPath.resolve(photo.getFilename())
                .normalize();

        if (!filePath.startsWith(uploadPath)
                || !Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Photo not found"
            );
        }

        MediaType type;

        if (photo.getFilename().endsWith(".jpg")) {
            type = MediaType.IMAGE_JPEG;
        } else if (photo.getFilename().endsWith(".png")) {
            type = MediaType.IMAGE_PNG;
        } else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Invalid photo format"
            );
        }

        try {
            byte[] bytes = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(type)
                    .contentLength(bytes.length)
                    .body(new ByteArrayResource(bytes));

        } catch (IOException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read photo"
            );
        }
    }

}

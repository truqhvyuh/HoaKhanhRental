
package com.hoakhanh.rental.service;

import com.hoakhanh.rental.dto.RoomPageResponse;
import com.hoakhanh.rental.dto.RoomResponse;
import com.hoakhanh.rental.entity.Room;
import com.hoakhanh.rental.entity.ModerationStatus;
import com.hoakhanh.rental.entity.OccupancyStatus;
import com.hoakhanh.rental.entity.VisibilityStatus;
import com.hoakhanh.rental.repository.RoomRepository;
import com.hoakhanh.rental.repository.RoomSpecification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public RoomPageResponse searchRooms(
            String keyword,
            String area,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minArea,
            BigDecimal maxArea,
            int page,
            int size,
            String sort
    ) {

        // 1. Kiem tra phan trang
        if (page < 0) {
            throw new ResponseStatusException(
                    BAD_REQUEST, "page must be >= 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "size must be between 1 and 100"
            );
        }

        // 2. Kiem tra gia va dien tich
        validateRange(minPrice, maxPrice, "price");
        validateRange(minArea, maxArea, "area");

        // 3. Sap xep
        Sort sorting = buildSort(sort);

        Pageable pageable = PageRequest.of(
                page,
                size,
                sorting
        );

        // 4. Giu nguyen bo loc tim kiem cu
        var specification = RoomSpecification.search(
                keyword,
                area,
                minPrice,
                maxPrice,
                minArea,
                maxArea
        );

        // 5. BO SUNG BO LOC PHONG CONG KHAI
        specification = specification.and(
                (root, query, cb) -> cb.and(

                        // Phong phai duoc ADMIN duyet
                        cb.equal(
                                root.get("moderationStatus"),
                                ModerationStatus.APPROVED
                        ),

                        // Phong phai dang hien thi
                        cb.equal(
                                root.get("visibilityStatus"),
                                VisibilityStatus.VISIBLE
                        ),

                        // Phong phai con trong
                        cb.equal(
                                root.get("occupancyStatus"),
                                OccupancyStatus.AVAILABLE
                        ),

                        // Phong chua bi xoa mem
                        cb.isNull(
                                root.get("deletedAt")
                        )
                )
        );

        // 6. Lay danh sach phong da loc
        Page<Room> roomPage = roomRepository.findAll(
                specification,
                pageable
        );

        // 7. Chuyen du lieu sang DTO
        Page<RoomResponse> responsePage = roomPage.map(
                room -> new RoomResponse(
                        room.getId(),
                        room.getTitle(),
                        room.getDescription(),
                        room.getAddress(),
                        room.getAreaName(),
                        room.getPrice(),
                        room.getAreaSqm(),
                        room.getMaxOccupants()
                )
        );

        return new RoomPageResponse(
                responsePage.getContent(),
                responsePage.getNumber(),
                responsePage.getSize(),
                responsePage.getTotalElements(),
                responsePage.getTotalPages(),
                responsePage.isFirst(),
                responsePage.isLast()
        );
    }

    private Sort buildSort(String sort) {

        Sort primarySort = switch (sort) {

            case "newest" ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "createdAt"
                    );

            case "oldest" ->
                    Sort.by(
                            Sort.Direction.ASC,
                            "createdAt"
                    );

            case "price_asc" ->
                    Sort.by(
                            Sort.Direction.ASC,
                            "price"
                    );

            case "price_desc" ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "price"
                    );

            case "area_asc" ->
                    Sort.by(
                            Sort.Direction.ASC,
                            "areaSqm"
                    );

            case "area_desc" ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "areaSqm"
                    );

            default -> throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Invalid sort value"
            );
        };

        return primarySort.and(
                Sort.by(
                        Sort.Direction.DESC,
                        "id"
                )
        );
    }

    private void validateRange(
            BigDecimal min,
            BigDecimal max,
            String field
    ) {

        if (min != null && min.signum() < 0) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    field + " minimum cannot be negative"
            );
        }

        if (max != null && max.signum() < 0) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    field + " maximum cannot be negative"
            );
        }

        if (min != null && max != null
                && min.compareTo(max) > 0) {

            throw new ResponseStatusException(
                    BAD_REQUEST,
                    field + " minimum exceeds maximum"
            );
        }
    }

    // XEM CHI TIET PHONG CONG KHAI
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {

        Room room = roomRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                NOT_FOUND,
                                "Room not found"
                        )
                );

        // Chi cho xem phong hop le
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
                    NOT_FOUND,
                    "Room not found"
            );
        }

        return new RoomResponse(
                room.getId(),
                room.getTitle(),
                room.getDescription(),
                room.getAddress(),
                room.getAreaName(),
                room.getPrice(),
                room.getAreaSqm(),
                room.getMaxOccupants()
        );
    }
}

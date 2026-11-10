
package com.hoakhanh.rental.controller;

import com.hoakhanh.rental.dto.RoomPageResponse;
import com.hoakhanh.rental.service.RoomService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import com.hoakhanh.rental.dto.RoomResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public RoomPageResponse getRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort
    ) {
        return roomService.searchRooms(
                keyword,
                area,
                minPrice,
                maxPrice,
                minArea,
                maxArea,
                page,
                size,
                sort
        );
    }

    @GetMapping("/{id}")
    public RoomResponse getRoomById(@PathVariable Long id) {
        return roomService.getRoomById(id);
    }

}


package com.hoakhanh.rental.dto;

import java.math.BigDecimal;

public record RoomResponse(
        Long id,
        String title,
        String description,
        String address,
        String areaName,
        BigDecimal price,
        BigDecimal areaSqm,
        Integer maxOccupants
) {
}

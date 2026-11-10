
package com.hoakhanh.rental.dto;

import java.util.List;

public record RoomPageResponse(
        List<RoomResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}

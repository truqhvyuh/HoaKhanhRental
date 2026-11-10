
package com.hoakhanh.rental.dto;

import com.hoakhanh.rental.entity.OccupancyStatus;
import com.hoakhanh.rental.entity.VisibilityStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateRoomStatusRequest(
        @NotNull VisibilityStatus visibilityStatus,
        @NotNull OccupancyStatus occupancyStatus
) {}

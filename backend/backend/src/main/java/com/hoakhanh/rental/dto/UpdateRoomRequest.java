
package com.hoakhanh.rental.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record UpdateRoomRequest(
        @NotBlank @Size(max = 200)
        String title,

        @NotBlank @Size(max = 255)
        String address,

        @Size(max = 150)
        String areaName,

        @NotNull @DecimalMin("0.01")
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull @DecimalMin("0.01")
        @Digits(integer = 6, fraction = 2)
        BigDecimal areaSqm,

        @NotNull @Min(1)
        Integer maxOccupants,

        String description
) {}

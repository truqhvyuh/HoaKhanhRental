
package com.hoakhanh.rental.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Ho ten khong duoc de trong")
        @Size(max = 150)
        String fullName,

        @NotBlank(message = "Email khong duoc de trong")
        @Email(message = "Email khong hop le")
        String email,

        @NotBlank(message = "Mat khau khong duoc de trong")
        @Size(min = 12, max = 72,
                message = "Mat khau phai co tu 12 den 72 ky tu")
        String password,

        @Size(max = 20)
        String phone
) {
}

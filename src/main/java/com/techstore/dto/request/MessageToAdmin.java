package com.techstore.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MessageToAdmin {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String name;

    private String phone;

    @NotBlank
    private String message;
}

package com.example.adsportalbe.dto.auth;

import com.example.adsportalbe.validation.ValidPassword;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequestDto {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @ValidPassword
    private String password;

    @AssertTrue(message = "You must be over 18 to register")
    private boolean isOver18;

    @AssertTrue(message = "You must accept the policies to register")
    private boolean acceptsPolicies;
}

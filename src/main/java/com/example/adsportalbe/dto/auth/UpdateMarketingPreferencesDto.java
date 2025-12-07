package com.example.adsportalbe.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMarketingPreferencesDto {

    @NotNull(message = "subscribedToMarketingEmails is required")
    private Boolean subscribedToMarketingEmails;
}

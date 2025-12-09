package com.example.adsportalbe.dto.ad;

import com.example.adsportalbe.enums.AdStatus;
import com.example.adsportalbe.models.ad.AdFormatType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdDto {
    private Long id;
    private String title;
    private AdFormatType formatType;
    private Integer viewsBought;
    private Double price;
    private LocalDateTime submittedDate;
    private LocalDateTime startDate;
    private String email;
    private Long userId;
    private AdStatus status;
}

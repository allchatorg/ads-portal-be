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
public class AdDetailedViewDto {
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
    private String textContent;
    private String imageUrl;
    private String videoUrl;
    private String cardBrand;
    private String cardLast4;
    private String rejectionReason;
}

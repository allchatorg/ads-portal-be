package com.example.adsportalbe.dto.requests;

import com.example.adsportalbe.enums.AdStatus;
import com.example.adsportalbe.models.ad.AdFormatType;

import java.time.LocalDateTime;
import java.util.List;

public record AdSearchRequestDto(
        AdStatus status,
        List<AdFormatType> types,
        int page,
        int size,
        String sort,
        Long userId,
        String email,
        LocalDateTime approvedAtStart,
        LocalDateTime approvedAtEnd) {
}

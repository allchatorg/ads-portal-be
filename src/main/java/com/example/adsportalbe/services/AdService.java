package com.example.adsportalbe.services;

import com.example.adsportalbe.dto.ad.AdDto;
import com.example.adsportalbe.dto.ad.AdStatusCountDto;
import com.example.adsportalbe.dto.ad.CreateAdRequestDto;
import com.example.adsportalbe.dto.requests.AdSearchRequestDto;
import com.example.adsportalbe.models.ad.Ad;
import com.example.adsportalbe.models.identity.User;
import com.stripe.exception.StripeException;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AdService {
    Ad createAd(CreateAdRequestDto request, User user) throws StripeException;

    Page<AdDto> searchAds(AdSearchRequestDto request);

    List<AdStatusCountDto> getAdStatusCounts();

    List<AdStatusCountDto> getAdStatusCountsByUserId(Long userId);
}

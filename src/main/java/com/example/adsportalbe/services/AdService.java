package com.example.adsportalbe.services;

import com.example.adsportalbe.dto.ad.CreateAdRequestDto;
import com.example.adsportalbe.models.ad.Ad;
import com.example.adsportalbe.models.identity.User;
import com.stripe.exception.StripeException;

public interface AdService {
    Ad createAd(CreateAdRequestDto request, User user) throws StripeException;
}

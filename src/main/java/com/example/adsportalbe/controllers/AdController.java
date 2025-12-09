package com.example.adsportalbe.controllers;

import com.example.adsportalbe.dto.ad.AdDto;
import com.example.adsportalbe.dto.ad.AdStatusCountDto;
import com.example.adsportalbe.dto.ad.CreateAdRequestDto;
import com.example.adsportalbe.dto.requests.AdSearchRequestDto;
import com.example.adsportalbe.models.ad.Ad;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.services.AdService;
import com.example.adsportalbe.services.UserService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<Void> createAd(@RequestBody CreateAdRequestDto request,
                                         @AuthenticationPrincipal UserDetails userDetails) throws StripeException {
        User user = userService.findByEmail(userDetails.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        Ad createdAd = adService.createAd(request, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<AdDto>> searchAds(@ModelAttribute AdSearchRequestDto request) {
        Page<AdDto> result = adService.searchAds(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status-counts")
    public ResponseEntity<List<AdStatusCountDto>> getAdStatusCounts() {
        List<AdStatusCountDto> result = adService.getAdStatusCounts();
        return ResponseEntity.ok(result);
    }
}

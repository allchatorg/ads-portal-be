package com.example.adsportalbe.controllers;

import com.example.adsportalbe.dto.ad.AdDetailedViewDto;
import com.example.adsportalbe.dto.ad.AdDto;
import com.example.adsportalbe.dto.ad.AdRejectionRequestDto;
import com.example.adsportalbe.dto.ad.AdStatusCountDto;
import com.example.adsportalbe.dto.requests.AdSearchRequestDto;
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
@RequestMapping("/api/admin/ads")
@RequiredArgsConstructor
public class AdminAdController {

    private final AdService adService;
    private final UserService userService;

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

    @GetMapping("/{id}")
    public ResponseEntity<AdDetailedViewDto> getAdById(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        AdDetailedViewDto result = adService.getAdById(id, user);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reject")
    public ResponseEntity<AdDetailedViewDto> rejectAd(@RequestBody AdRejectionRequestDto request)
            throws StripeException {
        AdDetailedViewDto result = adService.rejectAd(request.getAdId(), request.getRejectionReason());
        return ResponseEntity.ok(result);
    }
}

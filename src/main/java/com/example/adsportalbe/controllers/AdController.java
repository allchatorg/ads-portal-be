package com.example.adsportalbe.controllers;

import com.example.adsportalbe.dto.ServeAdRequestDto;
import com.example.adsportalbe.dto.ServedAdDto;
import com.example.adsportalbe.dto.ad.*;
import com.example.adsportalbe.dto.requests.AdSearchRequestDto;
import com.example.adsportalbe.enums.Role;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.services.AdService;
import com.example.adsportalbe.services.AdStatisticsService;
import com.example.adsportalbe.services.UserService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;
    private final UserService userService;
    private final AdStatisticsService adStatisticsService;

    @PostMapping
    public ResponseEntity<Void> createAd(@RequestBody CreateAdRequestDto request,
                                         @AuthenticationPrincipal UserDetails userDetails) throws StripeException {
        User user = userService.findByEmail(userDetails.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        adService.createAd(request, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<Page<AdDto>> searchAds(@ModelAttribute AdSearchRequestDto request,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        // For regular users (non-admin), enforce userId validation
        if (user.getRole() == Role.USER) {
            // Check if userId is provided
            if (request.userId() == null) {
                return ResponseEntity.status(403)
                        .body(null); // Regular users must provide userId
            }

            // Verify the userId matches the authenticated user
            if (!request.userId().equals(user.getId())) {
                return ResponseEntity.status(403)
                        .body(null); // Regular users can only query their own ads
            }
        }
        // For admins, no restrictions - they can query any userId or no userId

        Page<AdDto> result = adService.searchAds(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status-counts")
    public ResponseEntity<List<AdStatusCountDto>> getAdStatusCounts() {
        List<AdStatusCountDto> result = adService.getAdStatusCounts();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status-counts-by-user")
    public ResponseEntity<List<AdStatusCountDto>> getAdStatusCountsByUserId(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        Long userId = user.getId();
        List<AdStatusCountDto> result = adService.getAdStatusCountsByUserId(userId);

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

    @PostMapping("/serve")
    public ResponseEntity<ServedAdDto> serveAd(@RequestBody ServeAdRequestDto request) {
        // Optional: validation for request.getUserId() vs authenticated user if needed
        // But the requirements say "accept a userid" which implies it might be used by
        // a system or the user itself.
        // Given the optional IP, I'll pass it through.

        ServedAdDto servedAd = adStatisticsService.serveAd(request.getUserId(), request.getIpAddress());
        if (servedAd == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(servedAd);
    }

    @GetMapping("/{id}/daily-stats")
    public ResponseEntity<AdDailyStatsResponseDto> getAdDailyStats(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        if (user == null) {
            throw new RuntimeException("User not found");
        }

        AdDailyStatsResponseDto result = adStatisticsService.getAdDailyStats(id, fromDate, user);
        return ResponseEntity.ok(result);
    }
}

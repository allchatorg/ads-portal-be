package com.example.adsportalbe.controllers;

import com.example.adsportalbe.dto.ad.AdDto;
import com.example.adsportalbe.dto.ad.AdStatusCountDto;
import com.example.adsportalbe.dto.ad.CreateAdRequestDto;
import com.example.adsportalbe.dto.requests.AdSearchRequestDto;
import com.example.adsportalbe.enums.Role;
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

        // For regular users, only allow querying their own counts
        Long userId = user.getId();
        if (user.getRole() == Role.USER) {
            // Regular users can only see their own counts
            List<AdStatusCountDto> result = adService.getAdStatusCountsByUserId(userId);
            return ResponseEntity.ok(result);
        }

        // For admins, they could query any user's counts or all counts
        // For now, we'll return their own counts, but admins should use /status-counts
        // for all
        List<AdStatusCountDto> result = adService.getAdStatusCountsByUserId(userId);
        return ResponseEntity.ok(result);
    }
}

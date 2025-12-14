package com.example.adsportalbe.bootstrap;

import com.example.adsportalbe.enums.AdStatus;
import com.example.adsportalbe.enums.Role;
import com.example.adsportalbe.models.ad.Ad;
import com.example.adsportalbe.models.ad.AdFormat;
import com.example.adsportalbe.models.ad.AdFormatSnapshot;
import com.example.adsportalbe.models.ad.AdFormatType;
import com.example.adsportalbe.models.identity.User;
import com.example.adsportalbe.repositories.AdFormatRepository;
import com.example.adsportalbe.repositories.AdRepository;
import com.example.adsportalbe.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AdRepository adRepository;
    private final AdFormatRepository adFormatRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            log.info("Starting DataSeeding...");
            User user = seedUser();
            seedAdFormats();
            seedAds(user);
            log.info("DataSeeding completed successfully.");
        } catch (Exception e) {
            log.error("DataSeeding failed: {}", e.getMessage(), e);
        }
    }

    private User seedUser() {
        String email = "seed.user@example.com";
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Seeding user: {}", email);
                    User newUser = User.builder()
                            .firstName("Seed")
                            .lastName("User")
                            .email(email)
                            .password(passwordEncoder.encode("password"))
                            .role(Role.USER)
                            .emailVerified(true)
                            .subscribedToMarketingEmails(false)
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private void seedAdFormats() {
        if (adFormatRepository.count() == 0) {
            log.info("Seeding AdFormats...");
            List<AdFormat> formats = new ArrayList<>();

            formats.add(AdFormat.builder()
                    .type(AdFormatType.TEXT)
                    .title("Text Ad")
                    .description("Simple text-based advertisement.")
                    .pricePerMille(2.0)
                    .recommended(false)
                    .features(List.of("100 chars"))
                    .build());

            formats.add(AdFormat.builder()
                    .type(AdFormatType.PHOTO)
                    .title("Photo Ad")
                    .description("Image-based advertisement.")
                    .pricePerMille(5.0)
                    .recommended(true)
                    .features(List.of("1 Image", "Caption"))
                    .build());

            formats.add(AdFormat.builder()
                    .type(AdFormatType.VIDEO)
                    .title("Video Ad")
                    .description("Video-based advertisement.")
                    .pricePerMille(10.0)
                    .recommended(false)
                    .features(List.of("30s Video"))
                    .build());

            adFormatRepository.saveAll(formats);
        }
    }

    private void seedAds(User user) {
        if (adRepository.count() == 0) {
            log.info("Seeding Ads for user: {}", user.getEmail());

            AdFormat photoFormat = adFormatRepository.findByType(AdFormatType.PHOTO)
                    .orElseThrow(() -> new RuntimeException("Photo format missing"));

            AdFormat videoFormat = adFormatRepository.findByType(AdFormatType.VIDEO)
                    .orElseThrow(() -> new RuntimeException("Video format missing"));

            Ad photoAd = Ad.builder()
                    .title("Seed Photo Ad")
                    .owner(user)
                    .format(photoFormat)
                    .imageUrl("https://picsum.photos/seed/picsum/200/300")
                    .totalViewsBought(2)
                    .servedViews(0)
                    .totalCost(10.0) // Arbitrary cost
                    .status(AdStatus.ACTIVE)
                    .submittedAt(Instant.now())
                    .approvedAt(Instant.now())
                    .build();

            AdFormatSnapshot photoSnapshot = AdFormatSnapshot.builder()
                    .originalFormatId(photoFormat.getId())
                    .type(photoFormat.getType())
                    .title(photoFormat.getTitle())
                    .description(photoFormat.getDescription())
                    .pricePerMille(photoFormat.getPricePerMille())
                    .build();
            photoAd.setSelectedFormats(List.of(photoSnapshot));

            Ad videoAd = Ad.builder()
                    .title("Seed Video Ad")
                    .owner(user)
                    .format(videoFormat)
                    .videoUrl("https://www.w3schools.com/html/mov_bbb.mp4")
                    .totalViewsBought(5)
                    .servedViews(0)
                    .totalCost(50.0) // Arbitrary cost
                    .status(AdStatus.ACTIVE)
                    .submittedAt(Instant.now())
                    .approvedAt(Instant.now())
                    .build();

            AdFormatSnapshot videoSnapshot = AdFormatSnapshot.builder()
                    .originalFormatId(videoFormat.getId())
                    .type(videoFormat.getType())
                    .title(videoFormat.getTitle())
                    .description(videoFormat.getDescription())
                    .pricePerMille(videoFormat.getPricePerMille())
                    .build();
            videoAd.setSelectedFormats(List.of(videoSnapshot));

            adRepository.saveAll(List.of(photoAd, videoAd));
        }
    }
}

package com.example.adsportalbe.services;

import com.example.adsportalbe.dto.AdImpressionDto;
import com.example.adsportalbe.models.ad.Ad;
import com.example.adsportalbe.models.ad.AdImpression;
import com.example.adsportalbe.repositories.AdImpressionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdImpressionCacheService {

    private static final String IMPRESSION_KEY_PREFIX = "ad:impression:";
    private final RedisTemplate<String, Object> redisTemplate;
    private final AdImpressionRepository adImpressionRepository;
    private final AdService adService;

    /**
     * Caches a single ad impression in Redis.
     *
     * @param impressionDto the impression DTO to cache
     */
    public void cacheImpression(AdImpressionDto impressionDto) {
        if (impressionDto == null || impressionDto.getAdId() == null) {
            log.warn("Attempted to cache null impression or impression without ad ID");
            return;
        }

        String key = IMPRESSION_KEY_PREFIX + impressionDto.getAdId();
        redisTemplate.opsForList().rightPush(key, impressionDto);
        log.debug("Cached impression for ad ID: {}", impressionDto.getAdId());
    }

    /**
     * Retrieves all cached impressions for a specific ad.
     *
     * @param adId the ID of the ad
     * @return list of cached ad impression DTOs
     */
    public List<AdImpressionDto> getCachedImpressions(Long adId) {
        if (adId == null) {
            return List.of();
        }

        String key = IMPRESSION_KEY_PREFIX + adId;
        List<Object> objects = redisTemplate.opsForList().range(key, 0, -1);

        if (objects == null) {
            return List.of();
        }

        return objects.stream()
                .filter(obj -> obj instanceof AdImpressionDto)
                .map(obj -> (AdImpressionDto) obj)
                .collect(Collectors.toList());
    }

    /**
     * Clears cached impressions for a specific ad.
     *
     * @param adId the ID of the ad
     */
    public void clearCachedImpressions(Long adId) {
        if (adId == null) {
            return;
        }
        String key = IMPRESSION_KEY_PREFIX + adId;
        redisTemplate.delete(key);
        log.info("Cleared cached impressions for ad ID: {}", adId);
    }

    /**
     * Retrieves all ad impressions for a specific ad.
     *
     * @param adId the ID of the ad
     * @return list of ad impressions for the given ad ID
     */
    public List<AdImpression> getByAdId(Long adId) {
        if (adId == null) {
            log.warn("Attempted to retrieve impressions with null ad ID");
            return List.of();
        }

        log.debug("Retrieving impressions for ad ID: {}", adId);
        return adImpressionRepository.findByAd_Id(adId);
    }

    /**
     * Deletes all ad impressions for a specific ad.
     *
     * @param adId the ID of the ad whose impressions should be deleted
     */
    @Transactional
    public void deleteByAdId(Long adId) {
        if (adId == null) {
            log.warn("Attempted to delete impressions with null ad ID");
            return;
        }

        log.info("Deleting all impressions for ad ID: {}", adId);
        adImpressionRepository.deleteByAd_Id(adId);
        log.info("Successfully deleted impressions for ad ID: {}", adId);
    }

    /**
     * Creates multiple ad impressions from DTOs.
     *
     * @param impressionDtos list of ad impression DTOs
     * @return list of created ad impressions
     */
    @Transactional
    public List<AdImpression> createAdImpressions(List<AdImpressionDto> impressionDtos) {
        if (impressionDtos == null || impressionDtos.isEmpty()) {
            log.warn("Attempted to create impressions with null or empty list");
            return List.of();
        }

        log.info("Creating {} ad impressions", impressionDtos.size());

        // Extract unique ad IDs
        Set<Long> adIds = impressionDtos.stream()
                .map(AdImpressionDto::getAdId)
                .collect(Collectors.toSet());

        // Fetch all ads in batch
        Map<Long, Ad> adsById = adService.findAllById(adIds).stream()
                .collect(Collectors.toMap(Ad::getId, ad -> ad));

        // Build impressions
        List<AdImpression> impressions = impressionDtos.stream()
                .map(dto -> {
                    Ad ad = adsById.get(dto.getAdId());
                    if (ad == null) {
                        throw new RuntimeException("Ad not found with ID: " + dto.getAdId());
                    }

                    return AdImpression.builder()
                            .ad(ad)
                            .timestamp(dto.getTimestamp())
                            .ipAddress(dto.getIpAddress())
                            .userId(dto.getUserId())
                            .build();
                })
                .collect(Collectors.toList());

        // Save all impressions in batch
        List<AdImpression> savedImpressions = adImpressionRepository.saveAll(impressions);
        log.info("Successfully created {} ad impressions", savedImpressions.size());

        return savedImpressions;
    }
}

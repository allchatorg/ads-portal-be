package com.example.adsportalbe.services;

import com.example.adsportalbe.dto.CachedAd;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdCacheService {

    private static final String AD_ACTIVE_SET_KEY = "ad:active_set";
    private static final String AD_DETAILS_KEY_PREFIX = "ad:details:";
    private static final String AD_VIEWS_KEY_PREFIX = "ad:views:";
    private final RedisTemplate<String, Object> redisTemplate;

    public void cacheAd(CachedAd ad) {
        if (ad == null || ad.getId() == null) {
            log.warn("Attempted to cache null ad or ad without ID");
            return;
        }

        String detailsKey = AD_DETAILS_KEY_PREFIX + ad.getId();

        // Store details
        redisTemplate.opsForValue().set(detailsKey, ad);

        // Add ID to set of active ads
        redisTemplate.opsForSet().add(AD_ACTIVE_SET_KEY, ad.getId().toString());

        log.info("Cached ad with ID: {}", ad.getId());
    }

    public void removeAd(Long adId) {
        if (adId == null)
            return;

        String detailsKey = AD_DETAILS_KEY_PREFIX + adId;
        String viewsKey = AD_VIEWS_KEY_PREFIX + adId;

        // Remove details
        redisTemplate.delete(detailsKey);

        // Remove view count
        redisTemplate.delete(viewsKey);

        // Remove from set
        redisTemplate.opsForSet().remove(AD_ACTIVE_SET_KEY, adId.toString());

        log.info("Removed ad from cache with ID: {}", adId);
    }

    public CachedAd chooseRandomAd() {
        // Get random member from set
        Object randomIdObj = redisTemplate.opsForSet().randomMember(AD_ACTIVE_SET_KEY);

        if (randomIdObj == null) {
            log.debug("No active ads in cache");
            return null;
        }

        String adIdStr = randomIdObj.toString();
        String detailsKey = AD_DETAILS_KEY_PREFIX + adIdStr;

        Object adObj = redisTemplate.opsForValue().get(detailsKey);

        if (!(adObj instanceof CachedAd)) {
            throw new RuntimeException("Cached ad details not of type CachedAd");
        }
        if (adObj instanceof CachedAd) {
            CachedAd cachedAd = (CachedAd) adObj;
            long currentCachedViews = getViewCount(cachedAd.getId());
            int baseServedViews = cachedAd.getServedViews() != null ? cachedAd.getServedViews() : 0;
            cachedAd.setServedViews(baseServedViews + (int) currentCachedViews);
            return cachedAd;
        }

        // If details generic fetch failed or mismatched type
        log.warn("Could not retrieve details for ad ID: {}", adIdStr);
        // Clean up inconsistency
        removeAd(Long.valueOf(adIdStr));

        return null;
    }

    public void incrementViewCount(Long adId) {
        if (adId == null)
            return;

        String viewsKey = AD_VIEWS_KEY_PREFIX + adId;
        redisTemplate.opsForValue().increment(viewsKey);
        log.debug("Incremented view count for ad ID: {}", adId);
    }

    public long getViewCount(Long adId) {
        if (adId == null) {
            return 0;
        }
        String viewsKey = AD_VIEWS_KEY_PREFIX + adId;
        Object views = redisTemplate.opsForValue().get(viewsKey);
        if (views != null) {
            if (views instanceof Integer) {
                return ((Integer) views).longValue();
            } else if (views instanceof Long) {
                return (Long) views;
            } else {
                try {
                    return Long.parseLong(views.toString());
                } catch (NumberFormatException e) {
                    log.warn("Could not parse view count for ad ID: " + adId, e);
                    return 0;
                }
            }
        }
        return 0;
    }

    /**
     * Retrieves all active ad IDs from the cache.
     *
     * @return set of all active ad IDs
     */
    public Set<Long> getAllActiveAdIds() {
        Set<Object> members = redisTemplate.opsForSet().members(AD_ACTIVE_SET_KEY);
        if (members == null || members.isEmpty()) {
            return Set.of();
        }

        return members.stream()
                .map(obj -> {
                    try {
                        return Long.parseLong(obj.toString());
                    } catch (NumberFormatException e) {
                        log.warn("Could not parse ad ID from cache: {}", obj);
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
    }
}

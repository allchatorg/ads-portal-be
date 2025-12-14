package com.example.adsportalbe.services;

import com.example.adsportalbe.dto.CachedAd;
import com.example.adsportalbe.models.ad.AdFormatType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdCacheServiceIntegrationTest {

    @Autowired
    private AdCacheService adCacheService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testCacheAdAndRetrieveKeys() {
        // Arrange
        Long adId = 9999L;
        CachedAd ad = CachedAd.builder()
                .id(adId)
                .title("Test Ad")
                .format(AdFormatType.TEXT)
                .textContent("Test Content")
                .totalViewsBought(100)
                .servedViews(0)
                .build();

        // Act
        adCacheService.cacheAd(ad);

        // Assert
        // 1. Check if the Set key exists and contains the ID
        Boolean isMember = redisTemplate.opsForSet().isMember("ad:active_set", adId.toString());
        assertTrue(isMember, "Ad ID should be in 'ad:active_set'");

        // 2. Check if the Details key exists
        String detailsKey = "ad:details:" + adId;
        Object cachedObj = redisTemplate.opsForValue().get(detailsKey);
        assertNotNull(cachedObj, "Ad details should be present in Redis");
        assertTrue(cachedObj instanceof CachedAd, "Cached object should be of type CachedAd");
        assertEquals("Test Ad", ((CachedAd) cachedObj).getTitle());

        // Cleanup
        adCacheService.removeAd(adId);
    }
}

package com.example.adsportalbe.jobs;

import com.example.adsportalbe.dto.AdImpressionDto;
import com.example.adsportalbe.services.AdImpressionCacheService;
import com.example.adsportalbe.services.AdStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdImpressionProcessingJob {

    private final AdImpressionCacheService adImpressionCacheService;
    private final AdStatisticsService adStatisticsService;

    @Scheduled(fixedRate = 5000)
    public void processImpressionsFromCache() {
        log.trace("Starting scheduled ad impression processing");

        List<AdImpressionDto> impressions = adImpressionCacheService.popAllImpressions();

        if (impressions.isEmpty()) {
            return;
        }

        log.info("Cron job retrieved {} impressions from cache", impressions.size());

        try {
            adStatisticsService.processImpressions(impressions);
        } catch (Exception e) {
            log.error("Failed to process impressions from cron job", e);
            // In a real production system, we might want to push these back to Redis
            // or a dead letter queue to avoid data loss.
            // For now, we log the error.
        }
    }
}

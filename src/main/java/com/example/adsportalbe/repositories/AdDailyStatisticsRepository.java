package com.example.adsportalbe.repositories;

import com.example.adsportalbe.models.ad.AdDailyStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdDailyStatisticsRepository extends JpaRepository<AdDailyStatistics, Long> {
}

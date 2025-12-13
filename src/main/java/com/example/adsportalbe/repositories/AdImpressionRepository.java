package com.example.adsportalbe.repositories;

import com.example.adsportalbe.models.ad.AdImpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdImpressionRepository extends JpaRepository<AdImpression, Long> {
}

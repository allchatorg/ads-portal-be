package com.example.adsportalbe.repositories;

import com.example.adsportalbe.models.ad.AdFormat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdFormatRepository extends JpaRepository<AdFormat, Long> {
}

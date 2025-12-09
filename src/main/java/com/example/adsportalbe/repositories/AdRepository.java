package com.example.adsportalbe.repositories;

import com.example.adsportalbe.dto.ad.AdStatusCountDto;
import com.example.adsportalbe.models.ad.Ad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdRepository extends JpaRepository<Ad, Long>, JpaSpecificationExecutor<Ad> {

    @Query("SELECT new com.example.adsportalbe.dto.ad.AdStatusCountDto(a.status, COUNT(a)) FROM Ad a GROUP BY a.status")
    List<AdStatusCountDto> getAdStatusCounts();
}

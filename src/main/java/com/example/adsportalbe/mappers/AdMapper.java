package com.example.adsportalbe.mappers;


import com.example.adsportalbe.dto.ad.AdDto;
import com.example.adsportalbe.models.ad.Ad;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdMapper {

    @Mapping(source = "format.type", target = "formatType")
    @Mapping(source = "totalViewsBought", target = "viewsBought")
    @Mapping(source = "totalCost", target = "price")
    @Mapping(source = "submittedAt", target = "submittedDate")
    @Mapping(source = "approvedAt", target = "startDate")
    @Mapping(source = "owner.email", target = "email")
    @Mapping(source = "owner.id", target = "userId")
    AdDto toDto(Ad ad);
}

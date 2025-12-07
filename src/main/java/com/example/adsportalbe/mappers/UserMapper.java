package com.example.adsportalbe.mappers;

import com.example.adsportalbe.dto.UserDto;
import com.example.adsportalbe.dto.auth.AuthResponseDto;
import com.example.adsportalbe.models.identity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Maps User entity to UserDto, excluding password field
     */
    UserDto toUserDto(User user);

    /**
     * Maps User entity to AuthResponseDto for authentication responses
     * Note: accessToken and expiresIn must be set separately after mapping
     */
    @Mapping(target = "accessToken", ignore = true)
    @Mapping(target = "expiresIn", ignore = true)
    AuthResponseDto toAuthResponseDto(User user);
}
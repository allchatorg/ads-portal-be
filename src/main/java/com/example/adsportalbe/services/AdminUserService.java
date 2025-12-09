package com.example.adsportalbe.services;

import com.example.adsportalbe.dto.AdminUserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    Page<AdminUserDto> getAllUsers(Pageable pageable);

    AdminUserDto getUserById(Long id);
}

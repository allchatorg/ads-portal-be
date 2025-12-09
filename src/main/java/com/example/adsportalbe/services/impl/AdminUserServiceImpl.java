package com.example.adsportalbe.services.impl;

import com.example.adsportalbe.dto.AdminUserDto;
import com.example.adsportalbe.repositories.UserRepository;
import com.example.adsportalbe.services.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAllAdminUsers(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDto getUserById(Long id) {
        return userRepository.findAdminUserById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}

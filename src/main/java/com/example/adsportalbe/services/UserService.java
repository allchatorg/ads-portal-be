package com.example.adsportalbe.services;

import com.example.adsportalbe.models.identity.User;

public interface UserService {

    User findByEmail(String email);

    boolean existsByEmail(String email);
}

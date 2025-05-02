package com.urlshortener.service;

import com.urlshortener.dto.auth.JwtResponse;
import com.urlshortener.dto.auth.LoginRequest;
import com.urlshortener.dto.auth.RegisterRequest;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);
    JwtResponse register(RegisterRequest registerRequest);
    JwtResponse refreshToken(String refreshToken);
} 
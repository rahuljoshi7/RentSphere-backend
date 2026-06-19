package com.rentsphere.service;

import com.rentsphere.dto.request.LoginRequest;
import com.rentsphere.dto.request.RegisterRequest;
import com.rentsphere.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}

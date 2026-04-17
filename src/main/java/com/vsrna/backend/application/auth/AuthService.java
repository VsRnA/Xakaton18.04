package com.vsrna.backend.application.auth;

import com.vsrna.backend.presentation.dto.AuthDto;

public interface AuthService {

    AuthDto.LoginResponse login(AuthDto.LoginRequest request);

    AuthDto.LoginResponse register(AuthDto.RegisterRequest request);
}

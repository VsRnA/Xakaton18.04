package com.prodforge.backend.application.auth;

import com.prodforge.backend.presentation.dto.auth.AuthDto;

public interface AuthService {

    AuthDto.LoginResponse login(AuthDto.LoginRequest request);

    AuthDto.LoginResponse register(AuthDto.RegisterRequest request);
}

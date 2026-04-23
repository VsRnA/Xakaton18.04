package com.prodforge.backend.application.auth;

import com.prodforge.backend.application.user.CreateUserRequest;
import com.prodforge.backend.application.user.UserService;
import com.prodforge.backend.domain.user.User;
import com.prodforge.backend.infrastructure.security.JwtUtils;
import com.prodforge.backend.presentation.dto.auth.AuthDto;
import com.prodforge.backend.presentation.dto.user.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        User user = userService.validateCredentials(request.phone(), request.password());
        String token = jwtUtils.generateToken(user.getGuid(), user.getRoleKeywords(), user.getUsername());
        log.info("User logged in: userId={}, username={}", user.getGuid(), user.getUsername());
        return new AuthDto.LoginResponse(token, UserDto.UserResponse.from(user));
    }

    @Override
    @Transactional
    public AuthDto.LoginResponse register(AuthDto.RegisterRequest request) {
        User user = userService.createUser(new CreateUserRequest(
                request.phone(),
                request.password(),
                null,
                request.username()
        ));
        String token = jwtUtils.generateToken(user.getGuid(), user.getRoleKeywords(), user.getUsername());
        log.info("User registered: userId={}, username={}", user.getGuid(), user.getUsername());
        return new AuthDto.LoginResponse(token, UserDto.UserResponse.from(user));
    }
}

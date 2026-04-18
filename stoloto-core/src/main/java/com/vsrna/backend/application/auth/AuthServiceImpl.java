package com.vsrna.backend.application.auth;

import com.vsrna.backend.application.user.UserService;
import com.vsrna.backend.domain.user.User;
import com.vsrna.backend.infrastructure.security.JwtUtils;
import com.vsrna.backend.presentation.dto.AuthDto;
import com.vsrna.backend.presentation.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        User user = userService.validateCredentials(request.phone(), request.password());
        String token = jwtUtils.generateToken(user.getGuid(), user.getRoleKeywords());
        return new AuthDto.LoginResponse(token, UserDto.UserResponse.from(user));
    }

    @Override
    @Transactional
    public AuthDto.LoginResponse register(AuthDto.RegisterRequest request) {
        User user = userService.createUser(new UserDto.CreateUserRequest(
                request.phone(),
                request.password(),
                (String) null
        ));
        String token = jwtUtils.generateToken(user.getGuid(), user.getRoleKeywords());
        return new AuthDto.LoginResponse(token, UserDto.UserResponse.from(user));
    }
}

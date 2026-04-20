package com.vsrna.backend.application.user;

import com.vsrna.backend.domain.user.User;
import com.vsrna.backend.presentation.dto.UserDto;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User createUser(UserDto.CreateUserRequest request);

    User getUser(UUID guid);

    User updateUser(UUID guid, UserDto.UpdateUserRequest request);

    void deleteUser(UUID guid);

    List<User> listUsers(int limit, int offset);

    User validateCredentials(String phone, String password);
}

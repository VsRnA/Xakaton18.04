package com.vsrna.backend.application.user;

import com.vsrna.backend.domain.user.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    User createUser(CreateUserRequest request);

    User getUser(UUID guid);

    User updateUser(UUID guid, UpdateUserRequest request);

    void deleteUser(UUID guid);

    List<User> listUsers(int limit, int offset);

    User validateCredentials(String phone, String password);
}

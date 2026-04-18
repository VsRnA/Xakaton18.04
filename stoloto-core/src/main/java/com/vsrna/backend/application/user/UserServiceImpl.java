package com.vsrna.backend.application.user;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.role.Role;
import com.vsrna.backend.domain.role.RoleQuery;
import com.vsrna.backend.domain.role.RoleRepository;
import com.vsrna.backend.domain.user.User;
import com.vsrna.backend.domain.user.UserQuery;
import com.vsrna.backend.domain.user.UserRepository;
import com.vsrna.backend.domain.user.UserRole;
import com.vsrna.backend.presentation.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public User createUser(UserDto.CreateUserRequest request) {
        if (userRepository.find(UserQuery.byPhone(request.phone())).isPresent()) {
            throw ApiException.alreadyExists("User", "phone already taken");
        }

        User user = new User(request.phone(), passwordEncoder.encode(request.password()));

        String roleKeyword = (request.role() != null && !request.role().isBlank())
                ? UserRole.fromString(request.role()).getKeyword()
                : UserRole.USER.getKeyword();

        Role role = roleRepository.get(RoleQuery.byKeyword(roleKeyword));
        user.getRoles().add(role);

        return userRepository.create(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(UUID guid) {
        return userRepository.get(UserQuery.byId(guid));
    }

    @Override
    @Transactional
    public User updateUser(UUID guid, UserDto.UpdateUserRequest request) {
        User user = userRepository.get(UserQuery.byId(guid));

        if (request.username() != null && !request.username().isBlank()
                && !request.username().equals(user.getUsername())) {
            if (userRepository.find(UserQuery.byUsername(request.username())).isPresent()) {
                throw ApiException.alreadyExists("User", "username already taken");
            }
            user.setUsername(request.username());
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.name() != null) {
            user.setName(request.name());
        }

        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }

        if (request.patronymicName() != null) {
            user.setPatronymicName(request.patronymicName());
        }

        return userRepository.create(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID guid) {
        userRepository.get(UserQuery.byId(guid));
        userRepository.delete(UserQuery.byId(guid));
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listUsers(int limit, int offset) {
        return userRepository.list(UserQuery.list(limit, offset));
    }

    @Override
    @Transactional(readOnly = true)
    public User validateCredentials(String phone, String password) {
        User user = userRepository.find(UserQuery.byPhone(phone))
                .orElseThrow(() -> ApiException.unauthorized("invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw ApiException.unauthorized("invalid credentials");
        }

        return user;
    }
}

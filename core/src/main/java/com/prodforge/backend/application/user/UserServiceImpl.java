package com.prodforge.backend.application.user;

import com.prodforge.backend.domain.balance.UserBalance;
import com.prodforge.backend.domain.balance.UserBalanceRepository;
import com.prodforge.backend.domain.exception.ApiException;
import com.prodforge.backend.domain.role.Role;
import com.prodforge.backend.domain.role.RoleQuery;
import com.prodforge.backend.domain.role.RoleRepository;
import com.prodforge.backend.domain.user.User;
import com.prodforge.backend.domain.user.UserQuery;
import com.prodforge.backend.domain.user.UserRepository;
import com.prodforge.backend.domain.user.UserRole;
import com.prodforge.backend.application.port.PasswordEncoderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserBalanceRepository balanceRepository;
    private final PasswordEncoderPort passwordEncoder;

    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        if (userRepository.find(UserQuery.byPhone(request.phone())).isPresent()) {
            throw ApiException.alreadyExists("User", UserConstants.PHONE_ALREADY_TAKEN);
        }

        if (request.username() != null && !request.username().isBlank()
                && userRepository.find(UserQuery.byUsername(request.username())).isPresent()) {
            throw ApiException.alreadyExists("User", UserConstants.USERNAME_ALREADY_TAKEN);
        }

        User user = new User(request.phone(), passwordEncoder.encode(request.password()));
        if (request.username() != null && !request.username().isBlank()) {
            user.setUsername(request.username());
        }

        String roleKeyword = (request.role() != null && !request.role().isBlank())
                ? UserRole.fromString(request.role()).getKeyword()
                : UserRole.USER.getKeyword();

        Role role = roleRepository.get(RoleQuery.byKeyword(roleKeyword));
        user.getRoles().add(role);

        User created = userRepository.create(user);
        balanceRepository.create(new UserBalance(created.getGuid(), UserConstants.INITIAL_BALANCE));
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(UUID guid) {
        return userRepository.get(UserQuery.byId(guid));
    }

    @Override
    @Transactional
    public User updateUser(UUID guid, UpdateUserRequest request) {
        User user = userRepository.get(UserQuery.byId(guid));
        applyUsernameChange(user, request);
        applyPasswordChange(user, request);
        applyNameFields(user, request);
        applyRoleChange(user, request);
        return userRepository.create(user);
    }

    private void applyUsernameChange(User user, UpdateUserRequest request) {
        if (request.username() == null || request.username().isBlank()) return;
        if (request.username().equals(user.getUsername())) return;
        if (userRepository.find(UserQuery.byUsername(request.username())).isPresent()) {
            throw ApiException.alreadyExists("User", UserConstants.USERNAME_ALREADY_TAKEN);
        }
        user.setUsername(request.username());
    }

    private void applyPasswordChange(User user, UpdateUserRequest request) {
        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }
    }

    private void applyNameFields(User user, UpdateUserRequest request) {
        if (request.name() != null) user.setName(request.name());
        if (request.lastName() != null) user.setLastName(request.lastName());
        if (request.patronymicName() != null) user.setPatronymicName(request.patronymicName());
    }

    private void applyRoleChange(User user, UpdateUserRequest request) {
        if (request.role() == null || request.role().isBlank()) return;
        String roleKeyword = UserRole.fromString(request.role()).getKeyword();
        Role role = roleRepository.get(RoleQuery.byKeyword(roleKeyword));
        user.getRoles().clear();
        user.getRoles().add(role);
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

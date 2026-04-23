package com.vsrna.backend.infrastructure.persistence.user;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.role.Role;
import com.vsrna.backend.domain.user.User;
import com.vsrna.backend.domain.user.UserQuery;
import com.vsrna.backend.domain.user.UserRepository;
import com.vsrna.backend.infrastructure.persistence.role.RoleJpa;
import com.vsrna.backend.infrastructure.persistence.role.RoleRepositoryAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserJpaRepository jpa;
    private final RoleRepositoryAdapter roleRepositoryAdapter;

    @Override
    public User create(User user) {
        return toDomain(jpa.save(toJpa(user)));
    }

    @Override
    public Optional<User> find(UserQuery query) {
        return jpa.findByQuery(query.guid(), query.phone(), query.username())
                .map(this::toDomain);
    }

    @Override
    public User get(UserQuery query) {
        return find(query).orElseThrow(() -> ApiException.notFound("User", buildDetail(query)));
    }

    @Override
    public List<User> list(UserQuery query) {
        int limit = query.limit() > 0 ? query.limit() : 20;
        int offset = Math.max(query.offset(), 0);
        int page = limit > 0 ? offset / limit : 0;
        return jpa.findAllByOrderByCreatedAtDesc(PageRequest.of(page, limit))
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void delete(UserQuery query) {
        if (query.guid() != null) {
            jpa.deleteById(query.guid());
            return;
        }
        find(query).ifPresent(user -> jpa.deleteById(user.getGuid()));
    }

    private User toDomain(UserJpa j) {
        User user = new User(j.getPhone(), j.getPassword());
        user.setGuid(j.getGuid());
        user.setUsername(j.getUsername());
        user.setName(j.getName());
        user.setLastName(j.getLastName());
        user.setPatronymicName(j.getPatronymicName());
        user.setCreatedAt(j.getCreatedAt());
        user.setUpdatedAt(j.getUpdatedAt());
        Set<Role> roles = j.getRoles().stream()
                .map(roleRepositoryAdapter::toDomain)
                .collect(Collectors.toSet());
        user.setRoles(roles);
        return user;
    }

    private UserJpa toJpa(User user) {
        UserJpa j = new UserJpa();
        j.setGuid(user.getGuid());
        j.setPhone(user.getPhone());
        j.setUsername(user.getUsername());
        j.setPassword(user.getPassword());
        j.setName(user.getName());
        j.setLastName(user.getLastName());
        j.setPatronymicName(user.getPatronymicName());
        Set<RoleJpa> roleJpas = user.getRoles().stream()
                .map(role -> roleRepositoryAdapter.findJpaByKeyword(role.getKeyword()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
        j.setRoles(roleJpas);
        return j;
    }

    private String buildDetail(UserQuery query) {
        if (query.guid() != null) return query.guid().toString();
        if (query.phone() != null) return "phone=" + query.phone();
        if (query.username() != null) return "username=" + query.username();
        return "unknown";
    }
}

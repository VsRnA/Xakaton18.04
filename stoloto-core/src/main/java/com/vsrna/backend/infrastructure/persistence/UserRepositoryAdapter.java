package com.vsrna.backend.infrastructure.persistence;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.user.User;
import com.vsrna.backend.domain.user.UserQuery;
import com.vsrna.backend.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserJpaRepository jpa;

    @Override
    public User create(User user) {
        return jpa.save(user);
    }

    @Override
    public Optional<User> find(UserQuery query) {
        if (query.guid() != null) {
            return jpa.findById(query.guid());
        }
        if (query.phone() != null) {
            return jpa.findByPhone(query.phone());
        }
        if (query.username() != null) {
            return jpa.findByUsername(query.username());
        }
        return Optional.empty();
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
        return jpa.findAllByOrderByCreatedAtDesc(PageRequest.of(page, limit));
    }

    @Override
    public void delete(UserQuery query) {
        if (query.guid() != null) {
            jpa.deleteById(query.guid());
            return;
        }
        find(query).ifPresent(user -> jpa.deleteById(user.getGuid()));
    }

    private String buildDetail(UserQuery query) {
        if (query.guid() != null) return query.guid().toString();
        if (query.phone() != null) return "phone=" + query.phone();
        if (query.username() != null) return "username=" + query.username();
        return "unknown";
    }
}

package com.vsrna.backend.infrastructure.persistence;

import com.vsrna.backend.domain.exception.ApiException;
import com.vsrna.backend.domain.role.Role;
import com.vsrna.backend.domain.role.RoleQuery;
import com.vsrna.backend.domain.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final SpringDataRoleJpaRepository jpa;

    @Override
    public Role create(Role role) {
        return jpa.save(role);
    }

    @Override
    public Optional<Role> find(RoleQuery query) {
        if (query.guid() != null) {
            return jpa.findById(query.guid());
        }
        if (query.keyword() != null) {
            return jpa.findByKeyword(query.keyword());
        }
        return Optional.empty();
    }

    @Override
    public Role get(RoleQuery query) {
        return find(query).orElseThrow(() -> ApiException.notFound("Role", buildDetail(query)));
    }

    private String buildDetail(RoleQuery query) {
        if (query.guid() != null) return query.guid().toString();
        if (query.keyword() != null) return "keyword=" + query.keyword();
        return "unknown";
    }
}

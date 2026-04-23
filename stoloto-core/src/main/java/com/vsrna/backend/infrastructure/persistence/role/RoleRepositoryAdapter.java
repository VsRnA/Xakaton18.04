package com.vsrna.backend.infrastructure.persistence.role;

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
        return toDomain(jpa.save(toJpa(role)));
    }

    @Override
    public Optional<Role> find(RoleQuery query) {
        return jpa.findByQuery(query.guid(), query.keyword()).map(this::toDomain);
    }

    @Override
    public Role get(RoleQuery query) {
        return find(query).orElseThrow(() -> ApiException.notFound("Role", buildDetail(query)));
    }

    public Role toDomain(RoleJpa j) {
        return new Role(j.getGuid(), j.getKeyword(), j.getName());
    }

    RoleJpa toJpa(Role role) {
        RoleJpa j = new RoleJpa();
        j.setGuid(role.getGuid());
        j.setKeyword(role.getKeyword());
        j.setName(role.getName());
        return j;
    }

    public Optional<RoleJpa> findJpaByKeyword(String keyword) {
        return jpa.findByQuery(null, keyword);
    }

    private String buildDetail(RoleQuery query) {
        if (query.guid() != null) return query.guid().toString();
        if (query.keyword() != null) return "keyword=" + query.keyword();
        return "unknown";
    }
}

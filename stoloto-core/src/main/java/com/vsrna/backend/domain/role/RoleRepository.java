package com.vsrna.backend.domain.role;

import java.util.Optional;

public interface RoleRepository {

    Role create(Role role);

    Optional<Role> find(RoleQuery query);

    Role get(RoleQuery query);
}

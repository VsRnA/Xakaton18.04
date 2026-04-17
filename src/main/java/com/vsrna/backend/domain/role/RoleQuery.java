package com.vsrna.backend.domain.role;

import java.util.UUID;

public record RoleQuery(
        UUID guid,
        String keyword
) {

    public static RoleQuery byId(UUID guid) {
        return new RoleQuery(guid, null);
    }

    public static RoleQuery byKeyword(String keyword) {
        return new RoleQuery(null, keyword);
    }
}

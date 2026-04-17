package com.vsrna.backend.domain.user;

import java.util.UUID;

public record UserQuery(
        UUID guid,
        String phone,
        String username,
        int limit,
        int offset
) {

    public static UserQuery byId(UUID guid) {
        return new UserQuery(guid, null, null, 0, 0);
    }

    public static UserQuery byPhone(String phone) {
        return new UserQuery(null, phone, null, 0, 0);
    }

    public static UserQuery byUsername(String username) {
        return new UserQuery(null, null, username, 0, 0);
    }

    public static UserQuery list(int limit, int offset) {
        return new UserQuery(null, null, null, limit, offset);
    }
}

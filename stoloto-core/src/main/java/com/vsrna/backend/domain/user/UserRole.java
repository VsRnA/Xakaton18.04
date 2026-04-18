package com.vsrna.backend.domain.user;

public enum UserRole {
    USER("user", "Пользователь"),
    ADMIN("admin", "Администратор");

    private final String keyword;
    private final String name;

    UserRole(String keyword, String name) {
        this.keyword = keyword;
        this.name = name;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getName() {
        return name;
    }

    public static UserRole fromString(String role) {
        for (UserRole r : values()) {
            if (r.keyword.equalsIgnoreCase(role)) {
                return r;
            }
        }
        return USER;
    }
}

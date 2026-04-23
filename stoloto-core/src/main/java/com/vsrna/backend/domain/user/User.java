package com.vsrna.backend.domain.user;

import com.vsrna.backend.domain.role.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class User {

    private UUID guid;
    private String phone;
    private String username;
    private String password;
    private String name;
    private String lastName;
    private String patronymicName;
    private Set<Role> roles = new HashSet<>();
    private Instant createdAt;
    private Instant updatedAt;

    public User(String phone, String password) {
        this.phone = phone;
        this.password = password;
    }

    public Set<String> getRoleKeywords() {
        Set<String> keywords = new HashSet<>();
        for (Role role : roles) {
            keywords.add(role.getKeyword());
        }
        return keywords;
    }
}

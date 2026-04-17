package com.vsrna.backend.domain.user;

import com.vsrna.backend.domain.role.Role;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "guid", updatable = false, nullable = false)
    private UUID guid;

    @Column(name = "phone", length = 20, nullable = false, unique = true)
    private String phone;

    @Column(name = "username", length = 100, unique = true)
    private String username;

    @Column(name = "password", length = 255, nullable = false)
    private String password;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "lastName", length = 100)
    private String lastName;

    @Column(name = "patronymicName", length = 100)
    private String patronymicName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "userRoleAssigment",
            joinColumns = @JoinColumn(name = "userGuid"),
            inverseJoinColumns = @JoinColumn(name = "roleGuid")
    )
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
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

package com.vsrna.backend.infrastructure.persistence.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity(name = "Role")
@Table(name = "userRoles")
@Getter
@Setter
@NoArgsConstructor
public class RoleJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "guid", updatable = false, nullable = false)
    private UUID guid;

    @Column(name = "keyword", length = 50, nullable = false, unique = true)
    private String keyword;

    @Column(name = "name", length = 100, nullable = false)
    private String name;
}

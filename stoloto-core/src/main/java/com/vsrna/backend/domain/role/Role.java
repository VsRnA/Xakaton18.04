package com.vsrna.backend.domain.role;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "userRoles")
@Getter
@Setter
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "guid", updatable = false, nullable = false)
    private UUID guid;

    @Column(name = "keyword", length = 50, nullable = false, unique = true)
    private String keyword;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    public Role(String keyword, String name) {
        this.keyword = keyword;
        this.name = name;
    }
}

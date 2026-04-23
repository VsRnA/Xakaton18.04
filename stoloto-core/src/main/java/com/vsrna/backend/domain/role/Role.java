package com.vsrna.backend.domain.role;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class Role {

    private UUID guid;
    private String keyword;
    private String name;

    public Role(String keyword, String name) {
        this.keyword = keyword;
        this.name = name;
    }

    public Role(UUID guid, String keyword, String name) {
        this.guid = guid;
        this.keyword = keyword;
        this.name = name;
    }
}

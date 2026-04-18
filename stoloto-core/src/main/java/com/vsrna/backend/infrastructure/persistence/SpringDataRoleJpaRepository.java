package com.vsrna.backend.infrastructure.persistence;

import com.vsrna.backend.domain.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataRoleJpaRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByKeyword(String keyword);
}

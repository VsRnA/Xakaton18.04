package com.vsrna.backend.infrastructure.persistence.role;

import com.vsrna.backend.domain.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface SpringDataRoleJpaRepository extends JpaRepository<Role, UUID> {

    @Query("SELECT r FROM Role r WHERE (:guid IS NULL OR r.guid = :guid) AND (:keyword IS NULL OR r.keyword = :keyword)")
    Optional<Role> findByQuery(@Param("guid") UUID guid, @Param("keyword") String keyword);
}

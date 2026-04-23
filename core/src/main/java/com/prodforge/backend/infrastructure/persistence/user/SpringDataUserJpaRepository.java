package com.prodforge.backend.infrastructure.persistence.user;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataUserJpaRepository extends JpaRepository<UserJpa, UUID> {

    @Query("SELECT u FROM User u WHERE (:guid IS NULL OR u.guid = :guid) AND (:phone IS NULL OR u.phone = :phone) AND (:username IS NULL OR u.username = :username)")
    Optional<UserJpa> findByQuery(@Param("guid") UUID guid, @Param("phone") String phone, @Param("username") String username);

    List<UserJpa> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

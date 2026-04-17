package com.vsrna.backend.infrastructure.persistence;

import com.vsrna.backend.domain.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SpringDataUserJpaRepository extends JpaRepository<User, UUID> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByUsername(String username);

    List<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

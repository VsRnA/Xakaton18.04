package com.prodforge.backend.infrastructure.config;

import com.prodforge.backend.domain.role.Role;
import com.prodforge.backend.domain.role.RoleQuery;
import com.prodforge.backend.domain.role.RoleRepository;
import com.prodforge.backend.domain.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ApplicationRunner seedRoles(RoleRepository roleRepository) {
        return args -> {
            for (UserRole userRole : UserRole.values()) {
                if (roleRepository.find(RoleQuery.byKeyword(userRole.getKeyword())).isEmpty()) {
                    roleRepository.create(new Role(userRole.getKeyword(), userRole.getName()));
                }
            }
        };
    }
}

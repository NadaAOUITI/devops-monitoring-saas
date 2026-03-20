package com.n.devopsmonitoringsaas.repository;

import com.n.devopsmonitoringsaas.entity.User;
import com.n.devopsmonitoringsaas.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    List<User> findByTenantId(Long tenantId);

    List<User> findByTenantIdAndRole(Long tenantId, UserRole role);

    Optional<User> findByInvitationToken(String invitationToken);

    boolean existsByEmail(String email);
}

package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.User;
import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.exception.OwnerOperationException;
import com.n.devopsmonitoringsaas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> findByTenantId(Long tenantId) {
        return userRepository.findByTenantId(tenantId);
    }

    @Transactional
    public void deleteUser(Long tenantId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        if (user.getRole() == UserRole.OWNER) {
            throw new OwnerOperationException("Cannot delete the tenant owner");
        }

        userRepository.delete(user);
    }

    @Transactional
    public User updateRole(Long tenantId, Long userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (!user.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }

        if (user.getRole() == UserRole.OWNER) {
            throw new OwnerOperationException("Cannot change the owner's role");
        }

        user.setRole(newRole);
        return userRepository.save(user);
    }
}

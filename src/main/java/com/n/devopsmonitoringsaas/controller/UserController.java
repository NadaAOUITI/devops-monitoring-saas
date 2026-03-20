package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.User;
import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants/{tenantId}/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<User> listUsers(@PathVariable Long tenantId) {
        return userService.findByTenantId(tenantId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long tenantId, @PathVariable Long userId) {
        userService.deleteUser(tenantId, userId);
    }

    @PutMapping("/{userId}/role")
    public User updateRole(
            @PathVariable Long tenantId,
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return userService.updateRole(tenantId, userId, request.role());
    }

    public record UpdateRoleRequest(@NotNull UserRole role) {}
}

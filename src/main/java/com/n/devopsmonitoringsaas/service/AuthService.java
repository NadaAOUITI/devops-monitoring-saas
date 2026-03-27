package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.entity.User;
import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.exception.InvalidCredentialsException;
import com.n.devopsmonitoringsaas.exception.InvalidInvitationException;
import com.n.devopsmonitoringsaas.repository.PlanRepository;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import com.n.devopsmonitoringsaas.repository.UserRepository;
import com.n.devopsmonitoringsaas.security.JwtUtil;
import com.n.devopsmonitoringsaas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AuthService {

    private static final int INVITATION_EXPIRY_HOURS = 48;

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public String register(String email, String password, String companyName) {
        if (userRepository.existsByEmail(email)) {
            throw new InvalidCredentialsException("Email already registered");
        }

        Plan defaultPlan = planRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No plan available. Please create a plan first."));

        Tenant tenant = Tenant.builder()
                .name(companyName)
                .plan(defaultPlan)
                .build();
        tenant = tenantRepository.save(tenant);

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .role(UserRole.OWNER)
                .tenant(tenant)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        return jwtUtil.generateToken(user.getId(), tenant.getId(), user.getRole());
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("Account is not active. Please accept your invitation first.");
        }

        if (user.getPasswordHash() == null) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return jwtUtil.generateToken(user.getId(), user.getTenantId(), user.getRole());
    }

    @Transactional
    public InviteResponse invite(Long tenantId, String email, UserRole role) {
        UserRole inviterRole = SecurityUtils.currentUserRole()
                .orElseThrow(() -> new AccessDeniedException("Authentication required"));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Number jwtTenant) {
            if (jwtTenant.longValue() != tenantId) {
                throw new AccessDeniedException("Tenant mismatch");
            }
        }

        if (role == UserRole.OWNER && inviterRole != UserRole.OWNER) {
            throw new AccessDeniedException("Only OWNER can invite a user with OWNER role");
        }

        if (userRepository.existsByEmail(email)) {
            throw new InvalidCredentialsException("Email already registered");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        String invitationToken = UUID.randomUUID().toString();
        Instant invitationExpiresAt = Instant.now().plusSeconds(INVITATION_EXPIRY_HOURS * 3600L);

        User user = User.builder()
                .email(email)
                .role(role)
                .tenant(tenant)
                .invitationToken(invitationToken)
                .invitationExpiresAt(invitationExpiresAt)
                .isActive(false)
                .build();
        userRepository.save(user);

        return new InviteResponse(invitationToken, invitationExpiresAt);
    }

    @Transactional
    public String acceptInvite(String token, String password) {
        User user = userRepository.findByInvitationToken(token)
                .orElseThrow(() -> new InvalidInvitationException("Invalid or expired invitation"));

        if (user.getInvitationExpiresAt() == null || user.getInvitationExpiresAt().isBefore(Instant.now())) {
            throw new InvalidInvitationException("Invitation has expired");
        }

        user.setPasswordHash(passwordEncoder.encode(password));
        user.setInvitationToken(null);
        user.setInvitationExpiresAt(null);
        user.setIsActive(true);
        userRepository.save(user);

        return jwtUtil.generateToken(user.getId(), user.getTenantId(), user.getRole());
    }

    public record InviteResponse(String invitationToken, Instant invitationExpiresAt) {}
}

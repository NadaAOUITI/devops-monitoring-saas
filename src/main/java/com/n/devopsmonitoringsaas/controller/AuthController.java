package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
        String token = authService.register(request.email(), request.password(), request.companyName());
        return new TokenResponse(token);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.email(), request.password());
        return new TokenResponse(token);
    }

    @PostMapping("/invite")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('ROLE_OWNER','ROLE_ADMIN')")
    public AuthService.InviteResponse invite(@Valid @RequestBody InviteRequest request) {
        return authService.invite(request.tenantId(), request.email(), request.role());
    }

    @PostMapping("/accept-invite")
    public TokenResponse acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        String token = authService.acceptInvite(request.token(), request.password());
        return new TokenResponse(token);
    }

    public record TokenResponse(String token) {}

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String companyName) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record InviteRequest(
            @NotNull Long tenantId,
            @NotBlank @Email String email,
            @NotNull UserRole role) {}

    public record AcceptInviteRequest(
            @NotBlank String token,
            @NotBlank String password) {}
}

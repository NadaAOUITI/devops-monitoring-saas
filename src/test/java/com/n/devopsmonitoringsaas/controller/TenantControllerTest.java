package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.User;
import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.repository.UserRepository;
import com.n.devopsmonitoringsaas.security.JwtUtil;
import com.n.devopsmonitoringsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("TenantController")
class TenantControllerTest extends AbstractIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User ownerTenantA;
    private User ownerTenantB;

    @BeforeEach
    void registerTwoTenants() throws Exception {
        String emailA = "tenant-a-" + UUID.randomUUID() + "@example.com";
        String emailB = "tenant-b-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "email": "%s",
                                  "password": "SecretPass123!",
                                  "companyName": "Company A"
                                }
                                """, emailA)))
                .andExpect(status().isCreated());

        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "email": "%s",
                                  "password": "SecretPass123!",
                                  "companyName": "Company B"
                                }
                                """, emailB)))
                .andExpect(status().isCreated());

        ownerTenantA = userRepository.findByEmail(emailA).orElseThrow();
        ownerTenantB = userRepository.findByEmail(emailB).orElseThrow();
    }

    @Test
    @DisplayName("GET /tenants/{tenantId} — returns 200 when JWT tenant matches")
    void getTenant_success() throws Exception {
        String token = jwtUtil.generateToken(ownerTenantA.getId(), ownerTenantA.getTenant().getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}", ownerTenantA.getTenant().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ownerTenantA.getTenant().getId().intValue()))
                .andExpect(jsonPath("$.name").value("Company A"));
    }

    @Test
    @DisplayName("GET /tenants/{tenantId} — returns 403 when not authenticated")
    void getTenant_unauthorized() throws Exception {
        mockMvc.perform(get("/tenants/{tenantId}", ownerTenantA.getTenant().getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /tenants/{tenantId} — returns 403 when JWT tenant does not match path")
    void getTenant_differentTenantForbidden() throws Exception {
        String token = jwtUtil.generateToken(ownerTenantA.getId(), ownerTenantA.getTenant().getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}", ownerTenantB.getTenant().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /tenants/{tenantId}/webhook — returns 200 when owner sets webhook URL")
    void putWebhook_success() throws Exception {
        String token = jwtUtil.generateToken(ownerTenantA.getId(), ownerTenantA.getTenant().getId(), UserRole.OWNER);

        mockMvc.perform(put("/tenants/{tenantId}/webhook", ownerTenantA.getTenant().getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"webhookUrl\":\"https://hooks.example.com/alert\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.webhookUrl").value("https://hooks.example.com/alert"));
    }

    @Test
    @DisplayName("PUT /tenants/{tenantId}/webhook — returns 403 when caller is not OWNER")
    void putWebhook_nonOwnerForbidden() throws Exception {
        User member = User.builder()
                .email("member-tenant-a-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("pass"))
                .role(UserRole.MEMBER)
                .tenant(ownerTenantA.getTenant())
                .isActive(true)
                .build();
        member = userRepository.save(member);

        String token = jwtUtil.generateToken(member.getId(), member.getTenant().getId(), UserRole.MEMBER);

        mockMvc.perform(put("/tenants/{tenantId}/webhook", ownerTenantA.getTenant().getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"webhookUrl\":\"https://hooks.example.com/x\"}"))
                .andExpect(status().isForbidden());
    }
}

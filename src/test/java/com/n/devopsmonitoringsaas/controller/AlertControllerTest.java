package com.n.devopsmonitoringsaas.controller;

import com.n.devopsmonitoringsaas.entity.Alert;
import com.n.devopsmonitoringsaas.entity.AlertType;
import com.n.devopsmonitoringsaas.entity.Ping;
import com.n.devopsmonitoringsaas.entity.Service;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.entity.User;
import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.repository.AlertRepository;
import com.n.devopsmonitoringsaas.repository.PingRepository;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import com.n.devopsmonitoringsaas.repository.UserRepository;
import com.n.devopsmonitoringsaas.security.JwtUtil;
import com.n.devopsmonitoringsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AlertController")
class AlertControllerTest extends AbstractIntegrationTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private PingRepository pingRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User owner;
    private Tenant tenant;

    @BeforeEach
    void registerOwner() throws Exception {
        String email = "alert-owner-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "email": "%s",
                                  "password": "SecretPass123!",
                                  "companyName": "Alert Co"
                                }
                                """, email)))
                .andExpect(status().isCreated());

        owner = userRepository.findByEmail(email).orElseThrow();
        tenant = owner.getTenant();
    }

    private Alert seedAlert() {
        Service service = serviceRepository.save(Service.builder()
                .name("Monitored")
                .url("https://alert.example.com")
                .pingIntervalSeconds(60)
                .tenant(tenant)
                .build());

        Ping ping = pingRepository.save(Ping.builder()
                .service(service)
                .timestamp(Instant.now())
                .statusCode(500)
                .latencyMs(50)
                .isHealthy(false)
                .build());

        return alertRepository.save(Alert.builder()
                .ping(ping)
                .type(AlertType.DOWN)
                .message("failure")
                .sentAt(Instant.now())
                .build());
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/alerts — returns 200 and alerts for tenant")
    void listAlerts_success() throws Exception {
        seedAlert();

        String token = jwtUtil.generateToken(owner.getId(), tenant.getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}/alerts", tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/alerts/{alertId} — returns 200 when alert exists")
    void getAlert_success() throws Exception {
        Alert alert = seedAlert();

        String token = jwtUtil.generateToken(owner.getId(), tenant.getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}/alerts/{alertId}", tenant.getId(), alert.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alert.getId().intValue()));
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/alerts/{alertId} — returns 404 when alert does not exist")
    void getAlert_notFound() throws Exception {
        String token = jwtUtil.generateToken(owner.getId(), tenant.getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}/alerts/{alertId}", tenant.getId(), 999_999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /tenants/{tenantId}/alerts/{alertId}/acknowledge — returns 200 when owner acknowledges")
    void acknowledge_success() throws Exception {
        Alert alert = seedAlert();

        String token = jwtUtil.generateToken(owner.getId(), tenant.getId(), UserRole.OWNER);

        mockMvc.perform(put("/tenants/{tenantId}/alerts/{alertId}/acknowledge", tenant.getId(), alert.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledgedAt").exists());
    }

    @Test
    @DisplayName("PUT /tenants/{tenantId}/alerts/{alertId}/acknowledge — returns 403 when MEMBER tries to acknowledge")
    void acknowledge_memberForbidden() throws Exception {
        Alert alert = seedAlert();

        User member = userRepository.save(User.builder()
                .email("alert-member-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("pass"))
                .role(UserRole.MEMBER)
                .tenant(tenant)
                .isActive(true)
                .build());

        String token = jwtUtil.generateToken(member.getId(), member.getTenant().getId(), UserRole.MEMBER);

        mockMvc.perform(put("/tenants/{tenantId}/alerts/{alertId}/acknowledge", tenant.getId(), alert.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}

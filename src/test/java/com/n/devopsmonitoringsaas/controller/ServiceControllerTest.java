package com.n.devopsmonitoringsaas.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n.devopsmonitoringsaas.entity.User;
import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import com.n.devopsmonitoringsaas.repository.UserRepository;
import com.n.devopsmonitoringsaas.security.JwtUtil;
import com.n.devopsmonitoringsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ServiceController")
class ServiceControllerTest extends AbstractIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    private User ownerA;
    private User ownerB;

    @BeforeEach
    void registerTwoTenants() throws Exception {
        String emailA = "svc-a-" + UUID.randomUUID() + "@example.com";
        String emailB = "svc-b-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "email": "%s",
                                  "password": "SecretPass123!",
                                  "companyName": "Svc Co A"
                                }
                                """, emailA)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "email": "%s",
                                  "password": "SecretPass123!",
                                  "companyName": "Svc Co B"
                                }
                                """, emailB)))
                .andExpect(status().isCreated());

        ownerA = userRepository.findByEmail(emailA).orElseThrow();
        ownerB = userRepository.findByEmail(emailB).orElseThrow();
    }

    @Test
    @DisplayName("POST /tenants/{tenantId}/services — returns 201 when owner registers a service")
    void createService_success() throws Exception {
        String token = jwtUtil.generateToken(ownerA.getId(), ownerA.getTenant().getId(), UserRole.OWNER);

        mockMvc.perform(post("/tenants/{tenantId}/services", ownerA.getTenant().getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "API",
                                  "url": "https://example.com/health",
                                  "pingIntervalSeconds": 120
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("API"));
    }

    @Test
    @DisplayName("POST /tenants/{tenantId}/services — returns 403 when plan service limit is exceeded")
    void createService_planLimitExceeded() throws Exception {
        String token = jwtUtil.generateToken(ownerA.getId(), ownerA.getTenant().getId(), UserRole.OWNER);

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/tenants/{tenantId}/services", ownerA.getTenant().getId())
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("""
                                    {
                                      "name": "S%d",
                                      "url": "https://example.com/%d",
                                      "pingIntervalSeconds": 300
                                    }
                                    """, i, i)))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(post("/tenants/{tenantId}/services", ownerA.getTenant().getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Too many",
                                  "url": "https://example.com/extra",
                                  "pingIntervalSeconds": 300
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /tenants/{tenantId}/services — returns 403 when not authenticated")
    void createService_unauthorized() throws Exception {
        mockMvc.perform(post("/tenants/{tenantId}/services", ownerA.getTenant().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "X",
                                  "url": "https://example.com",
                                  "pingIntervalSeconds": 60
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/services — returns 200 and services for same tenant")
    void listServices_success() throws Exception {
        String token = jwtUtil.generateToken(ownerA.getId(), ownerA.getTenant().getId(), UserRole.OWNER);

        mockMvc.perform(post("/tenants/{tenantId}/services", ownerA.getTenant().getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Listed",
                                  "url": "https://listed.example.com",
                                  "pingIntervalSeconds": 60
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/tenants/{tenantId}/services", ownerA.getTenant().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Listed"));
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/services — returns 403 when JWT tenant does not match path")
    void listServices_differentTenantForbidden() throws Exception {
        String token = jwtUtil.generateToken(ownerA.getId(), ownerA.getTenant().getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}/services", ownerB.getTenant().getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /tenants/{tenantId}/services/{serviceId} — returns 200 when owner updates")
    void updateService_success() throws Exception {
        String token = jwtUtil.generateToken(ownerA.getId(), ownerA.getTenant().getId(), UserRole.OWNER);

        String createResponse = mockMvc.perform(post("/tenants/{tenantId}/services", ownerA.getTenant().getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "To Update",
                                  "url": "https://update.example.com",
                                  "pingIntervalSeconds": 60
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(createResponse);
        long serviceId = node.get("id").asLong();

        mockMvc.perform(put("/tenants/{tenantId}/services/{serviceId}", ownerA.getTenant().getId(), serviceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Name",
                                  "url": "https://update.example.com/v2",
                                  "pingIntervalSeconds": 90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    @DisplayName("PUT /tenants/{tenantId}/services/{serviceId} — returns 403 when not authenticated")
    void updateService_unauthorized() throws Exception {
        mockMvc.perform(put("/tenants/{tenantId}/services/{serviceId}", ownerA.getTenant().getId(), 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "X",
                                  "url": "https://x.com",
                                  "pingIntervalSeconds": 60
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /tenants/{tenantId}/services/{serviceId} — returns 204 when owner deletes")
    void deleteService_success() throws Exception {
        String token = jwtUtil.generateToken(ownerA.getId(), ownerA.getTenant().getId(), UserRole.OWNER);

        String createResponse = mockMvc.perform(post("/tenants/{tenantId}/services", ownerA.getTenant().getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "To Delete",
                                  "url": "https://delete.example.com",
                                  "pingIntervalSeconds": 60
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(createResponse);
        long serviceId = node.get("id").asLong();

        mockMvc.perform(delete("/tenants/{tenantId}/services/{serviceId}", ownerA.getTenant().getId(), serviceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertThat(serviceRepository.findById(serviceId)).isEmpty();
    }

    @Test
    @DisplayName("DELETE /tenants/{tenantId}/services/{serviceId} — returns 403 when not authenticated")
    void deleteService_unauthorized() throws Exception {
        mockMvc.perform(delete("/tenants/{tenantId}/services/{serviceId}", ownerA.getTenant().getId(), 999L))
                .andExpect(status().isForbidden());
    }
}

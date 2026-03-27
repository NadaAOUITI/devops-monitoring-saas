package com.n.devopsmonitoringsaas.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.n.devopsmonitoringsaas.entity.Incident;
import com.n.devopsmonitoringsaas.entity.IncidentCause;
import com.n.devopsmonitoringsaas.entity.IncidentStatus;
import com.n.devopsmonitoringsaas.entity.Service;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.entity.User;
import com.n.devopsmonitoringsaas.entity.UserRole;
import com.n.devopsmonitoringsaas.repository.IncidentRepository;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import com.n.devopsmonitoringsaas.repository.UserRepository;
import com.n.devopsmonitoringsaas.security.JwtUtil;
import com.n.devopsmonitoringsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("IncidentController")
class IncidentControllerTest extends AbstractIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServiceRepository serviceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    private User owner;
    private Tenant tenant;

    @BeforeEach
    void registerOwner() throws Exception {
        String email = "incident-owner-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                                {
                                  "email": "%s",
                                  "password": "SecretPass123!",
                                  "companyName": "Incident Co"
                                }
                                """, email)))
                .andExpect(status().isCreated());

        owner = userRepository.findByEmail(email).orElseThrow();
        tenant = owner.getTenant();
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/incidents — returns 200 and incidents for tenant")
    void listIncidents_success() throws Exception {
        Service s1 = serviceRepository.save(Service.builder()
                .name("S1")
                .url("https://inc1.example.com")
                .pingIntervalSeconds(60)
                .tenant(tenant)
                .build());

        incidentRepository.save(Incident.builder()
                .service(s1)
                .tenant(tenant)
                .status(IncidentStatus.CLOSED)
                .cause(IncidentCause.DOWN)
                .openedAt(Instant.parse("2024-06-01T12:00:00Z"))
                .closedAt(Instant.parse("2024-06-01T13:00:00Z"))
                .build());

        String token = jwtUtil.generateToken(owner.getId(), tenant.getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}/incidents", tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/incidents — ordered by openedAt descending (newest first)")
    void listIncidents_orderedNewestFirst() throws Exception {
        Service s1 = serviceRepository.save(Service.builder()
                .name("S-a")
                .url("https://a.example.com")
                .pingIntervalSeconds(60)
                .tenant(tenant)
                .build());
        Service s2 = serviceRepository.save(Service.builder()
                .name("S-b")
                .url("https://b.example.com")
                .pingIntervalSeconds(60)
                .tenant(tenant)
                .build());

        Incident older = incidentRepository.save(Incident.builder()
                .service(s1)
                .tenant(tenant)
                .status(IncidentStatus.CLOSED)
                .cause(IncidentCause.DEGRADED)
                .openedAt(Instant.parse("2024-01-01T10:00:00Z"))
                .closedAt(Instant.parse("2024-01-02T10:00:00Z"))
                .build());

        Incident newer = incidentRepository.save(Incident.builder()
                .service(s2)
                .tenant(tenant)
                .status(IncidentStatus.CLOSED)
                .cause(IncidentCause.DOWN)
                .openedAt(Instant.parse("2024-06-15T10:00:00Z"))
                .closedAt(Instant.parse("2024-06-15T11:00:00Z"))
                .build());

        String token = jwtUtil.generateToken(owner.getId(), tenant.getId(), UserRole.OWNER);

        String json = mockMvc.perform(get("/tenants/{tenantId}/incidents", tenant.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode arr = objectMapper.readTree(json);
        assertThat(arr).hasSize(2);
        assertThat(arr.get(0).get("id").asLong()).isEqualTo(newer.getId());
        assertThat(arr.get(1).get("id").asLong()).isEqualTo(older.getId());
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/incidents/{incidentId} — returns 200 when incident exists")
    void getIncident_success() throws Exception {
        Service s1 = serviceRepository.save(Service.builder()
                .name("S-one")
                .url("https://one.example.com")
                .pingIntervalSeconds(60)
                .tenant(tenant)
                .build());

        Incident inc = incidentRepository.save(Incident.builder()
                .service(s1)
                .tenant(tenant)
                .status(IncidentStatus.OPEN)
                .cause(IncidentCause.DOWN)
                .openedAt(Instant.now())
                .build());

        String token = jwtUtil.generateToken(owner.getId(), tenant.getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}/incidents/{incidentId}", tenant.getId(), inc.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inc.getId().intValue()));
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/incidents/{incidentId} — returns 404 when incident does not exist")
    void getIncident_notFound() throws Exception {
        String token = jwtUtil.generateToken(owner.getId(), tenant.getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}/incidents/{incidentId}", tenant.getId(), 999_999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /tenants/{tenantId}/services/{serviceId}/incidents — returns 200")
    void listServiceIncidents_success() throws Exception {
        Service s1 = serviceRepository.save(Service.builder()
                .name("S-filter")
                .url("https://filter.example.com")
                .pingIntervalSeconds(60)
                .tenant(tenant)
                .build());

        incidentRepository.save(Incident.builder()
                .service(s1)
                .tenant(tenant)
                .status(IncidentStatus.CLOSED)
                .cause(IncidentCause.SLOW)
                .openedAt(Instant.now().minusSeconds(3600))
                .closedAt(Instant.now())
                .build());

        String token = jwtUtil.generateToken(owner.getId(), tenant.getId(), UserRole.OWNER);

        mockMvc.perform(get("/tenants/{tenantId}/services/{serviceId}/incidents", tenant.getId(), s1.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}

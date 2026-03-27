package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.entity.Service;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.exception.PlanLimitExceededException;
import com.n.devopsmonitoringsaas.metrics.MetricsService;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import com.n.devopsmonitoringsaas.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private ServiceService serviceService;

    private Plan plan;
    private Tenant tenant;
    private Service service;

    @BeforeEach
    void setUp() {
        plan = Plan.builder()
                .id(1L)
                .name("Pro")
                .maxServices(5)
                .minPingIntervalSeconds(60)
                .build();

        tenant = Tenant.builder()
                .id(10L)
                .name("Acme Corp")
                .plan(plan)
                .createdAt(Instant.now())
                .build();

        service = Service.builder()
                .name("API Gateway")
                .url("https://api.example.com")
                .pingIntervalSeconds(60)
                .tenant(tenant)
                .build();
    }

    @Nested
    @DisplayName("registerService")
    class RegisterService {

        @Test
        @DisplayName("successfully registers a service when tenant exists and plan limit not reached")
        void registersService_whenTenantExistsAndLimitNotReached() {
            when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));
            when(serviceRepository.countByTenantId(10L)).thenReturn(2L);
            when(serviceRepository.save(any(Service.class))).thenAnswer(inv -> {
                Service s = inv.getArgument(0);
                s.setId(100L);
                return s;
            });

            Service result = serviceService.registerService(service);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getName()).isEqualTo("API Gateway");
            assertThat(result.getUrl()).isEqualTo("https://api.example.com");

            verify(tenantRepository).findById(10L);
            verify(serviceRepository).countByTenantId(10L);
            verify(serviceRepository).save(any(Service.class));
        }

        @Test
        @DisplayName("throws IllegalArgumentException when tenant is null")
        void throws_whenTenantIsNull() {
            service.setTenant(null);

            assertThatThrownBy(() -> serviceService.registerService(service))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Service must have a tenant with id");

            verify(tenantRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when tenant has no ID")
        void throws_whenTenantHasNoId() {
            tenant.setId(null);
            service.setTenant(tenant);

            assertThatThrownBy(() -> serviceService.registerService(service))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Service must have a tenant with id");

            verify(tenantRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when tenant is not found")
        void throws_whenTenantNotFound() {
            when(tenantRepository.findById(10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> serviceService.registerService(service))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Tenant not found");

            verify(tenantRepository).findById(10L);
            verify(serviceRepository, never()).countByTenantId(any(Long.class));
        }

        @Test
        @DisplayName("throws PlanLimitExceededException when tenant has reached max_services limit")
        void throws_whenPlanLimitReached() {
            when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));
            when(serviceRepository.countByTenantId(10L)).thenReturn(5L);

            assertThatThrownBy(() -> serviceService.registerService(service))
                    .isInstanceOf(PlanLimitExceededException.class)
                    .hasMessageContaining("Service limit reached")
                    .hasMessageContaining("max 5 services")
                    .hasMessageContaining("tenant has 5");

            verify(tenantRepository).findById(10L);
            verify(serviceRepository).countByTenantId(10L);
            verify(serviceRepository, never()).save(any(Service.class));
        }

        @Test
        @DisplayName("throws PlanLimitExceededException when count equals max_services")
        void throws_whenCountEqualsMaxServices() {
            when(tenantRepository.findById(10L)).thenReturn(Optional.of(tenant));
            when(serviceRepository.countByTenantId(10L)).thenReturn(5L);

            assertThatThrownBy(() -> serviceService.registerService(service))
                    .isInstanceOf(PlanLimitExceededException.class);

            verify(serviceRepository).countByTenantId(10L);
            verify(serviceRepository, never()).save(any(Service.class));
        }
    }

    @Nested
    @DisplayName("findByTenantId")
    class FindByTenantId {

        @Test
        @DisplayName("delegates to repository")
        void delegatesToRepository() {
            var services = java.util.List.of(service);
            when(serviceRepository.findByTenantId(10L)).thenReturn(services);

            var result = serviceService.findByTenantId(10L);

            assertThat(result).hasSize(1).containsExactly(service);
            verify(serviceRepository).findByTenantId(10L);
        }
    }
}

package com.n.devopsmonitoringsaas.service;

import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.entity.Tenant;
import com.n.devopsmonitoringsaas.exception.PlanLimitExceededException;
import com.n.devopsmonitoringsaas.repository.PlanRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plan subscription / change behaviour lives in {@link TenantService} (there is no separate PlanService).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TenantService (plan subscription)")
class TenantServicePlanTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private TenantService tenantService;

    private Tenant tenant;
    private Plan freePlan;
    private Plan proPlan;

    @BeforeEach
    void setUp() {
        freePlan = Plan.builder().id(1L).name("Free").maxServices(3).minPingIntervalSeconds(300).build();
        proPlan = Plan.builder().id(2L).name("Pro").maxServices(20).minPingIntervalSeconds(60).build();

        tenant = Tenant.builder().id(100L).name("Acme").plan(freePlan).build();
    }

    @Nested
    @DisplayName("subscribeToPlan")
    class Subscribe {

        @Test
        @DisplayName("succeeds when tenant and plan exist and service count fits")
        void success() {
            when(tenantRepository.findById(100L)).thenReturn(Optional.of(tenant));
            when(planRepository.findById(2L)).thenReturn(Optional.of(proPlan));
            when(serviceRepository.countByTenantId(100L)).thenReturn(2L);
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

            Tenant result = tenantService.subscribeToPlan(100L, 2L);

            assertThat(result.getPlan()).isEqualTo(proPlan);
            verify(tenantRepository).save(tenant);
        }

        @Test
        @DisplayName("throws when tenant not found")
        void tenantNotFound() {
            when(tenantRepository.findById(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.subscribeToPlan(100L, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tenant not found");

            verify(planRepository, never()).findById(any());
        }

        @Test
        @DisplayName("throws when plan not found")
        void planNotFound() {
            when(tenantRepository.findById(100L)).thenReturn(Optional.of(tenant));
            when(planRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> tenantService.subscribeToPlan(100L, 99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Plan not found");

            verify(tenantRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws when downgrading would violate new max_services")
        void downgradeBlockedWhenTooManyServices() {
            tenant.setPlan(proPlan);
            when(tenantRepository.findById(100L)).thenReturn(Optional.of(tenant));
            when(planRepository.findById(1L)).thenReturn(Optional.of(freePlan));
            when(serviceRepository.countByTenantId(100L)).thenReturn(5L);

            assertThatThrownBy(() -> tenantService.subscribeToPlan(100L, 1L))
                    .isInstanceOf(PlanLimitExceededException.class)
                    .hasMessageContaining("allows max 3");

            verify(tenantRepository, never()).save(any());
        }

        @Test
        @DisplayName("upgrade from Free to Pro when service count within Pro limits")
        void upgrade() {
            when(tenantRepository.findById(100L)).thenReturn(Optional.of(tenant));
            when(planRepository.findById(2L)).thenReturn(Optional.of(proPlan));
            when(serviceRepository.countByTenantId(100L)).thenReturn(0L);
            when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> inv.getArgument(0));

            Tenant result = tenantService.subscribeToPlan(100L, 2L);

            assertThat(result.getPlan().getName()).isEqualTo("Pro");
        }
    }
}

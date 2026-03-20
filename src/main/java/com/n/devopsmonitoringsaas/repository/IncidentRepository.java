package com.n.devopsmonitoringsaas.repository;

import com.n.devopsmonitoringsaas.entity.Incident;
import com.n.devopsmonitoringsaas.entity.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findByServiceIdAndStatus(Long serviceId, IncidentStatus status);

    List<Incident> findByTenantIdOrderByOpenedAtDesc(Long tenantId);

    List<Incident> findByServiceIdAndTenantIdOrderByOpenedAtDesc(Long serviceId, Long tenantId);
}

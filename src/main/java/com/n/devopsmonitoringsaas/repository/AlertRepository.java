package com.n.devopsmonitoringsaas.repository;

import com.n.devopsmonitoringsaas.entity.Alert;
import com.n.devopsmonitoringsaas.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByPingId(Long pingId);

    List<Alert> findByPingServiceIdAndType(Long serviceId, AlertType type);

    List<Alert> findByPingServiceTenantIdOrderBySentAtDesc(Long tenantId);

    List<Alert> findByPingServiceIdAndPingServiceTenantIdOrderBySentAtDesc(Long serviceId, Long tenantId);

    Optional<Alert> findByIdAndPingServiceTenantId(Long id, Long tenantId);
}

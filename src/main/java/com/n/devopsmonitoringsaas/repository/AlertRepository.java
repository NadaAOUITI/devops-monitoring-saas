package com.n.devopsmonitoringsaas.repository;

import com.n.devopsmonitoringsaas.entity.Alert;
import com.n.devopsmonitoringsaas.entity.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    @Query("""
            SELECT DISTINCT a FROM Alert a
            JOIN FETCH a.ping p
            JOIN FETCH p.service s
            JOIN FETCH s.tenant t
            LEFT JOIN FETCH p.incident i
            WHERE a.id = :id
            """)
    Optional<Alert> findWithDetailsById(@Param("id") Long id);

    List<Alert> findByPingId(Long pingId);

    List<Alert> findByPingServiceIdAndType(Long serviceId, AlertType type);

    @Query("""
            SELECT DISTINCT a FROM Alert a
            JOIN FETCH a.ping p
            JOIN FETCH p.service s
            JOIN FETCH s.tenant t
            LEFT JOIN FETCH p.incident i
            WHERE t.id = :tenantId
            ORDER BY a.sentAt DESC
            """)
    List<Alert> findByPingServiceTenantIdOrderBySentAtDesc(@Param("tenantId") Long tenantId);

    @Query("""
            SELECT DISTINCT a FROM Alert a
            JOIN FETCH a.ping p
            JOIN FETCH p.service s
            JOIN FETCH s.tenant t
            LEFT JOIN FETCH p.incident i
            WHERE s.id = :serviceId AND t.id = :tenantId
            ORDER BY a.sentAt DESC
            """)
    List<Alert> findByPingServiceIdAndPingServiceTenantIdOrderBySentAtDesc(
            @Param("serviceId") Long serviceId, @Param("tenantId") Long tenantId);

    @Query("""
            SELECT a FROM Alert a
            JOIN FETCH a.ping p
            JOIN FETCH p.service s
            JOIN FETCH s.tenant t
            LEFT JOIN FETCH p.incident i
            WHERE a.id = :id AND t.id = :tenantId
            """)
    Optional<Alert> findByIdAndPingServiceTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}

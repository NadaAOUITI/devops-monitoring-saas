package com.n.devopsmonitoringsaas.repository;

import com.n.devopsmonitoringsaas.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ServiceRepository extends JpaRepository<Service, Long> {

    List<Service> findByTenantId(Long tenantId);

    long countByTenantId(Long tenantId);

    boolean existsByTenantIdAndName(Long tenantId, String name);

    boolean existsByIdAndTenantId(Long id, Long tenantId);

    @Query(value = """
            SELECT s.id, s.name, s.url, s.max_latency_ms,
                   p.status_code, p.latency_ms, p.is_healthy, p.timestamp,
                   COALESCE(inc.cnt, 0), COALESCE(uptime.healthy_cnt, 0), COALESCE(uptime.total_cnt, 0)
            FROM services s
            LEFT JOIN LATERAL (
                SELECT status_code, latency_ms, is_healthy, timestamp
                FROM pings WHERE service_id = s.id ORDER BY timestamp DESC LIMIT 1
            ) p ON true
            LEFT JOIN (
                SELECT service_id, COUNT(*)::bigint AS cnt FROM incidents
                WHERE status = 'OPEN' GROUP BY service_id
            ) inc ON inc.service_id = s.id
            LEFT JOIN (
                SELECT service_id,
                       SUM(CASE WHEN is_healthy THEN 1 ELSE 0 END)::bigint AS healthy_cnt,
                       COUNT(*)::bigint AS total_cnt
                FROM pings WHERE timestamp >= :since GROUP BY service_id
            ) uptime ON uptime.service_id = s.id
            WHERE s.tenant_id = :tenantId
            """, nativeQuery = true)
    List<Object[]> findDashboardDataByTenantId(@Param("tenantId") Long tenantId, @Param("since") Instant since);
}

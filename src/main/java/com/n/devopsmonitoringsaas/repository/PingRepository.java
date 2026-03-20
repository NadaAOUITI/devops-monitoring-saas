package com.n.devopsmonitoringsaas.repository;

import com.n.devopsmonitoringsaas.entity.Ping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PingRepository extends JpaRepository<Ping, Long> {

    @Query("SELECT p FROM Ping p WHERE p.service.id = :serviceId " +
            "AND (:from IS NULL OR p.timestamp >= :from) " +
            "AND (:to IS NULL OR p.timestamp <= :to) " +
            "AND (:healthy IS NULL OR p.isHealthy = :healthy) " +
            "ORDER BY p.timestamp DESC")
    List<Ping> findByServiceIdWithFilters(
            @Param("serviceId") Long serviceId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("healthy") Boolean healthy);

    long countByServiceId(Long serviceId);
}

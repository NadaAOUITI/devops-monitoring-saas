package com.n.devopsmonitoringsaas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Service service;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "is_healthy", nullable = false)
    private Boolean isHealthy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Incident incident;

    @OneToMany(mappedBy = "ping", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<Alert> alerts = new ArrayList<>();
}

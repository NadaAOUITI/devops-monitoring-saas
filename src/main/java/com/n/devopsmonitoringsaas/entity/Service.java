package com.n.devopsmonitoringsaas.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "ping_interval_seconds", nullable = false)
    private Integer pingIntervalSeconds;

    @Column(name = "max_latency_ms")
    @Builder.Default
    private Integer maxLatencyMs = 2000;

    @Column(name = "tenant_id", nullable = false, insertable = false, updatable = false)
    private Long tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Tenant tenant;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private List<Ping> pings = new ArrayList<>();
}

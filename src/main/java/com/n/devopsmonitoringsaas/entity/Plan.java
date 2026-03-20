package com.n.devopsmonitoringsaas.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "plans")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "max_services", nullable = false)
    private Integer maxServices;

    @Column(name = "min_ping_interval_seconds", nullable = false)
    private Integer minPingIntervalSeconds;

    @ElementCollection
    @CollectionTable(name = "plan_allowed_alert_channels", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "channel")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private List<AlertChannel> allowedAlertChannels = new ArrayList<>();
}

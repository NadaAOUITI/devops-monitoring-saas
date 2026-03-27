package com.n.devopsmonitoringsaas.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "alerts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"ping"})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ping_id", nullable = false)
    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    private Ping ping;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private AlertType type;

    @Column(name = "message", nullable = false, length = 1024)
    private String message;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;
}

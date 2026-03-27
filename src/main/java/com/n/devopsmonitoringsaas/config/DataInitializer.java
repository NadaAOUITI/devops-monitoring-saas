package com.n.devopsmonitoringsaas.config;

import com.n.devopsmonitoringsaas.entity.AlertChannel;
import com.n.devopsmonitoringsaas.entity.Plan;
import com.n.devopsmonitoringsaas.repository.PlanRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final PlanRepository planRepository;

    @PostConstruct
    void initPlans() {
        if (planRepository.count() > 0) {
            return;
        }

        planRepository.save(Plan.builder()
                .name("Free")
                .maxServices(3)
                .minPingIntervalSeconds(300)
                .allowedAlertChannels(List.of(AlertChannel.EMAIL))
                .build());

        planRepository.save(Plan.builder()
                .name("Pro")
                .maxServices(20)
                .minPingIntervalSeconds(60)
                .allowedAlertChannels(List.of(AlertChannel.EMAIL, AlertChannel.WEBHOOK))
                .build());

        planRepository.save(Plan.builder()
                .name("Enterprise")
                .maxServices(100)
                .minPingIntervalSeconds(10)
                .allowedAlertChannels(List.of(AlertChannel.EMAIL, AlertChannel.WEBHOOK))
                .build());
    }
}

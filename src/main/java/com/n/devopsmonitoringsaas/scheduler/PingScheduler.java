package com.n.devopsmonitoringsaas.scheduler;

import com.n.devopsmonitoringsaas.entity.Service;
import com.n.devopsmonitoringsaas.repository.ServiceRepository;
import com.n.devopsmonitoringsaas.service.PingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class PingScheduler {

    private static final String LAST_PINGED_KEY_PREFIX = "last_pinged:";

    private final StringRedisTemplate redisTemplate;
    private final ServiceRepository serviceRepository;
    private final PingService pingService;

    @Scheduled(fixedRate = 10000)
    public void pingServices() {
        var services = serviceRepository.findAll();

        for (Service service : services) {
            long now = System.currentTimeMillis();
            String key = LAST_PINGED_KEY_PREFIX + service.getId();
            String lastPingedStr = redisTemplate.opsForValue().get(key);

            long elapsedSeconds;
            if (lastPingedStr == null) {
                elapsedSeconds = service.getPingIntervalSeconds() + 1;
            } else {
                long lastPingedMs = Long.parseLong(lastPingedStr);
                elapsedSeconds = (now - lastPingedMs) / 1000;
            }

            if (elapsedSeconds >= service.getPingIntervalSeconds()) {
                pingService.ping(service);
                redisTemplate.opsForValue().set(key, String.valueOf(now));
            }
        }
    }
}

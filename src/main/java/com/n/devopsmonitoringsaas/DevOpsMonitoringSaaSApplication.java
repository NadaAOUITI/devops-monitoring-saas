package com.n.devopsmonitoringsaas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DevOpsMonitoringSaaSApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevOpsMonitoringSaaSApplication.class, args);
    }

}

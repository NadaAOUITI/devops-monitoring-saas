package com.n.devopsmonitoringsaas.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ServiceCreateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255)
        String name,

        @NotBlank(message = "URL is required")
        @Size(max = 2048)
        String url,

        @NotNull(message = "Ping interval is required")
        @Min(1)
        Integer pingIntervalSeconds
) {}

package com.bachelorprojekt_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record DeviceStatusDTO(
        @NotNull Instant readAt,
        @NotNull @Min(0) @Max(100) Integer batteryLevel,
        @NotNull Boolean locked,
        @NotNull Boolean doorOpen,
        @NotNull Boolean isOnline) {
}

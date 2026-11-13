package com.bachelorprojekt_backend.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class StatusReadingState {
    private Integer batteryLevel;
    private Boolean locked;
    private Boolean doorOpen;
    private Boolean isOnline;
}

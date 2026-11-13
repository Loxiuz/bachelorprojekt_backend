package com.bachelorprojekt_backend.types;

public enum AlarmType {
    LOW_BATTERY,
    DOOR_OPEN_TOO_LONG,
    DEVICE_OFFLINE, //Hvis enheden ikke har sendt data i 10 minutter
    DEVICE_ERROR
}

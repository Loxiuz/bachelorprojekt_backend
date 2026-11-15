package com.bachelorprojekt_backend.security;

import jakarta.servlet.http.HttpServletRequest;

public final class DeviceStatusRequest {
    private DeviceStatusRequest() {
    }

    public static boolean matches(HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && request.getRequestURI().equals(request.getContextPath() + "/status");
    }
}

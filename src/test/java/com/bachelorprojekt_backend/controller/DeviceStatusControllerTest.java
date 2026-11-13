package com.bachelorprojekt_backend.controller;

import com.bachelorprojekt_backend.dto.DeviceStatusDTO;
import com.bachelorprojekt_backend.service.DeviceStatusService;
import com.bachelorprojekt_backend.security.DevicePrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DeviceStatusControllerTest {
    private final DeviceStatusService service = mock(DeviceStatusService.class);
    private final org.springframework.test.web.servlet.MockMvc mvc =
            MockMvcBuilders.standaloneSetup(new DeviceStatusController(service)).build();
    private final UUID deviceId = UUID.randomUUID();
    private final TestingAuthenticationToken auth = new TestingAuthenticationToken(new DevicePrincipal(deviceId), null);

    @Test
    void acceptsSimulatorSnapshot() throws Exception {
        String body = """
                {"readAt":"2026-01-01T12:00:00Z",
                 "batteryLevel":75,"locked":true,"doorOpen":false,"isOnline":true}
                """;

        mvc.perform(post("/status").principal(auth).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        var saved = org.mockito.ArgumentCaptor.forClass(DeviceStatusDTO.class);
        verify(service).saveDeviceStatus(saved.capture(), eq(deviceId));
        assertEquals(Instant.parse("2026-01-01T12:00:00Z"), saved.getValue().readAt());
        assertTrue(saved.getValue().isOnline());
    }

    @Test
    void rejectsIncompleteSnapshot() throws Exception {
        mvc.perform(post("/status").principal(auth).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}

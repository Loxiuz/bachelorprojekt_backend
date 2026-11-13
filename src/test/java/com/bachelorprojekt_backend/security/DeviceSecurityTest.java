package com.bachelorprojekt_backend.security;

import com.bachelorprojekt_backend.config.SpringSecurityConfig;
import com.bachelorprojekt_backend.controller.DeviceStatusController;
import com.bachelorprojekt_backend.model.Device;
import com.bachelorprojekt_backend.repository.DeviceRepository;
import com.bachelorprojekt_backend.service.DeviceStatusService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitConfig(DeviceSecurityTest.TestConfig.class)
@WebAppConfiguration
@TestPropertySource(properties = {"app.jwt-expiration-ms=3600000", "app.jwt-secret=test-secret"})
class DeviceSecurityTest {
    private static final String TOKEN = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String BODY = """
            {"readAt":"2026-01-01T12:00:00Z","batteryLevel":75,
             "locked":true,"doorOpen":false,"isOnline":true}
            """;

    @Autowired private WebApplicationContext context;
    @Autowired @Qualifier("springSecurityFilterChain") private Filter springSecurityFilterChain;
    @Autowired private CustomStatelessCsrfFilter csrf;
    @Autowired private DeviceRepository devices;
    @Autowired private DeviceStatusService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        reset(devices, service);
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(csrf, springSecurityFilterChain).build();
    }

    @Test
    void acceptsValidCredentialAndIgnoresSpoofedIds() throws Exception {
        UUID id = UUID.randomUUID();
        Device device = new Device();
        device.setId(id);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(TOKEN.getBytes(StandardCharsets.US_ASCII)));
        when(devices.findByCredentialHash(hash)).thenReturn(Optional.of(device));
        String spoofedBody = BODY.replace("{", "{\"deviceId\":\"" + UUID.randomUUID()
                + "\",\"tenantId\":\"" + UUID.randomUUID() + "\",");

        mvc.perform(post("/status").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(spoofedBody))
                .andExpect(status().isNoContent());
        verify(service).saveDeviceStatus(any(), eq(id));
    }

    @Test
    void rejectsMissingInvalidAndRevokedCredentials() throws Exception {
        mvc.perform(post("/status").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/status").header("Authorization", "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/status").cookie(new jakarta.servlet.http.Cookie("accessToken", "user-token"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/status").header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void deviceCredentialsDoNotBypassCsrfOrAuthenticateOtherEndpoints() throws Exception {
        mvc.perform(post("/other").header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isForbidden());
        mvc.perform(post("/other").header("Authorization", "Bearer " + TOKEN)
                        .cookie(new jakarta.servlet.http.Cookie("csrfToken", "test"))
                        .header("X-CSRF-Token", "test"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(devices, service);
    }

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    @Import(SpringSecurityConfig.class)
    static class TestConfig {
        @Bean DeviceRepository deviceRepository() { return mock(DeviceRepository.class); }
        @Bean DeviceStatusService deviceStatusService() { return mock(DeviceStatusService.class); }
        @Bean DeviceStatusController controller(DeviceStatusService service) { return new DeviceStatusController(service); }
        @Bean JwtAuthenticationEntryPoint entryPoint() { return new JwtAuthenticationEntryPoint(); }
        @Bean CustomStatelessCsrfFilter csrfFilter() { return new CustomStatelessCsrfFilter(); }
        @Bean JwtTokenProvider jwtTokenProvider() { return mock(JwtTokenProvider.class); }
        @Bean UserDetailsService userDetailsService() { return mock(UserDetailsService.class); }
    }
}

package com.bachelorprojekt_backend.security;

import com.bachelorprojekt_backend.repository.DeviceRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

public class DeviceAuthenticationFilter extends OncePerRequestFilter {
    private static final Pattern BEARER = Pattern.compile("^Bearer ([A-Za-z0-9_-]{43})$");
    private final DeviceRepository devices;

    public DeviceAuthenticationFilter(DeviceRepository devices) {
        this.devices = devices;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        String header = request.getHeader("Authorization");
        var matcher = BEARER.matcher(header == null ? "" : header);
        if (matcher.matches()) {
            devices.findByCredentialHash(hash(matcher.group(1))).ifPresent(device -> {
                var authentication = new UsernamePasswordAuthenticationToken(
                        new DevicePrincipal(device.getId()), null, List.of(new SimpleGrantedAuthority("DEVICE")));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}

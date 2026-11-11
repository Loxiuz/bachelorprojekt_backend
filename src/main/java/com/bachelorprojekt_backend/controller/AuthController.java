package com.bachelorprojekt_backend.controller;

import com.bachelorprojekt_backend.dto.UserPermissionsDTO;
import com.bachelorprojekt_backend.dto.LoginDTO;
import com.bachelorprojekt_backend.service.AuthService;
import com.bachelorprojekt_backend.security.CustomStatelessCsrfFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginDTO loginDto, HttpServletResponse response) {
        String token = authService.login(loginDto);

        Cookie accessToken = new Cookie("accessToken",  token);
        accessToken.setHttpOnly(true);
        accessToken.setSecure(true);
        accessToken.setPath("/");
        accessToken.setMaxAge(60 * 60);

        response.addCookie(accessToken);
        response.addCookie(buildCsrfCookie(UUID.randomUUID().toString(), 60 * 60));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody LoginDTO loginDto) {
        if(Boolean.FALSE.equals(authService.register(loginDto))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error registering user");
        }
        return ResponseEntity.ok("Registration successful");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletResponse response) {
        Cookie accessToken = new Cookie("accessToken", null);
        accessToken.setHttpOnly(true);
        accessToken.setSecure(true);
        accessToken.setPath("/");
        accessToken.setMaxAge(0);

        response.addCookie(accessToken);
        response.addCookie(buildCsrfCookie(null, 0));

        return ResponseEntity.ok("Logout successful");
    }

    @GetMapping("/logged-in")
    public ResponseEntity<Map<String, Boolean>> isLoggedIn() {
        boolean loggedIn = authService.isLoggedIn();

        Map<String, Boolean> response = new HashMap<>();
        response.put("loggedIn", loggedIn);

        return ResponseEntity.ok(response);

    }

    @GetMapping("/me/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserPermissionsDTO> getCurrentUserPermissions() {
        UserPermissionsDTO tenantUserPermissionsDTO = authService.getCurrentUserPermissions();
        if(tenantUserPermissionsDTO == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().body(tenantUserPermissionsDTO);
    }

    private Cookie buildCsrfCookie(String value, int maxAgeSeconds) {
        Cookie csrfCookie = new Cookie(CustomStatelessCsrfFilter.CSRF_COOKIE_NAME, value);
        csrfCookie.setHttpOnly(false);
        csrfCookie.setSecure(true);
        csrfCookie.setPath("/");
        csrfCookie.setMaxAge(maxAgeSeconds);
        return csrfCookie;
    }

}

package com.bachelorprojekt_backend.service;

import com.bachelorprojekt_backend.security.JwtTokenProvider;
import com.bachelorprojekt_backend.dto.UserPermissionsDTO;
import com.bachelorprojekt_backend.dto.LoginDTO;
import com.bachelorprojekt_backend.model.Permission;
import com.bachelorprojekt_backend.model.User;
import com.bachelorprojekt_backend.repository.RoleRepository;
import com.bachelorprojekt_backend.repository.UserRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private JwtTokenProvider jwtTokenProvider;

    public AuthService(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuthenticationManager authenticationManager ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.jwtTokenProvider = jwtTokenProvider;

    }

    public String login(LoginDTO loginDto) {
        // Delegates credential verification to Spring Security's authentication pipeline.
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginDto.getUsername(), loginDto.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtTokenProvider.generateToken(authentication);

        userRepository.findByUsername(jwtTokenProvider.getUsername(jwt))
                .orElseThrow(() -> new BadCredentialsException(""));

        return jwt;
    }

    public boolean isLoggedIn() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }


    public UserPermissionsDTO getCurrentUserPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            throw new BadCredentialsException("");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException(""));

        Set<Permission> permissions = roleRepository.findPermissionsByRoleId(user.getRole().getId());
        if(permissions.isEmpty()) {
            throw new IllegalArgumentException("No permissions found for role " + user.getRole().getRoleName());
        }

        return new UserPermissionsDTO(user.getRole().getRoleName(), username, permissions.stream().map(Permission::getPermissionName).toList(), user.getTenant().getId());
    }

    public Boolean register(LoginDTO loginDto) {
        if((loginDto.getUsername() == null || loginDto.getPassword() == null) ||
                (loginDto.getUsername().isEmpty() || loginDto.getPassword().isEmpty())) {
            throw new IllegalArgumentException("Username or password must not be empty");
        }
        if(userRepository.findByUsername(loginDto.getUsername()).orElse(null) != null) {
            throw new IllegalArgumentException("Username already exists");
        }
        User user = new User();
        user.setUsername(loginDto.getUsername());
        user.setPassword(new BCryptPasswordEncoder().encode(loginDto.getPassword()));
        user.setRole(roleRepository.findRoleByRoleName("ROLE_GUEST"));

        return userRepository.save(user).getId() != null;

    }
}

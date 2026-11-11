package com.bachelorprojekt_backend.config;

import com.bachelorprojekt_backend.security.CustomStatelessCsrfFilter;
import com.bachelorprojekt_backend.security.JwtAuthenticationEntryPoint;
import com.bachelorprojekt_backend.security.JwtAuthenticationFilter;
import com.bachelorprojekt_backend.security.JwtTokenProvider;
import lombok.AllArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@AllArgsConstructor
@EnableMethodSecurity
public class SpringSecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final CustomStatelessCsrfFilter customStatelessCsrfFilter;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Bean
    public static PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
        http
                // CORS rules are enforced here before request authorization runs.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                .csrf(AbstractHttpConfigurer::disable)
                // All security headers (CSP, X-Frame-Options, etc.) handled by nginx
                .headers(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> {
                     // Auth endpoints stay public; all other endpoints require authentication.
                     authorize.requestMatchers("/auth/**").permitAll();
                     authorize.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                // Stateless API: every request must carry valid authentication (JWT cookie).
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Ensures JWT auth runs before Spring's username/password authentication filter.
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                // Double-submit cookie CSRF validation for unsafe methods.
                .addFilterBefore(customStatelessCsrfFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Open CORS for development/exam setup.
        config.setAllowedOrigins(List.of("https://localhost"));
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS", "PATCH",  "DELETE", "PUT"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    AuthenticationManagerBuilder authenticationManagerBuilder(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class);
    }

     @Bean
     AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
         return authenticationManagerBuilder(http).build();
     }
}

package com.bachelorprojekt_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class CustomStatelessCsrfFilter extends OncePerRequestFilter {

    public static final String CSRF_COOKIE_NAME = "csrfToken";
    public static final String CSRF_HEADER_NAME = "X-CSRF-Token";

    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name()
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (shouldSkipCsrfCheck(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String csrfCookieToken = getCookieValue(request);
        String csrfHeaderToken = request.getHeader(CSRF_HEADER_NAME);

        if (!StringUtils.hasText(csrfCookieToken) || !csrfCookieToken.equals(csrfHeaderToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid CSRF token\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkipCsrfCheck(HttpServletRequest request) {
        if (SAFE_METHODS.contains(request.getMethod()) || DeviceStatusRequest.matches(request)) {
            return true;
        }

        String path = request.getRequestURI();
        return path.startsWith("/auth/login")
                || path.startsWith("/auth/register")
                || path.startsWith("/auth/logout");
    }

    private String getCookieValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (CustomStatelessCsrfFilter.CSRF_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}


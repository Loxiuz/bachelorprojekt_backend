package com.bachelorprojekt_backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // get JWT token from request
        String token = getTokenFromRequest(request);

        // validate token
        if(StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)){

            // get username from token
            String username = jwtTokenProvider.getUsername(token);

            // load the user associated with token
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // This marks the request as authenticated for @PreAuthorize and other security checks.
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        }

        filterChain.doFilter(request, response);
    }

     public String getTokenFromRequest(HttpServletRequest request){
        String token = null;
         if (request.getCookies() != null) {
             // Access token is expected as HttpOnly cookie named accessToken.
             for (Cookie cookie : request.getCookies()) {
                 if ("accessToken".equals(cookie.getName())) {
                     token = cookie.getValue();
                     break;
                 }
             }
         }

         if (token == null || !jwtTokenProvider.validateToken(token)) {
             // Controllers/services can inspect this attribute for diagnostics.
             request.setAttribute("error", "Invalid Token");
         }

        if(StringUtils.hasText(token)){

            return token;
        }
        return null;
    }
}

package com.example.lib.auth.util;

import com.example.lib.auth.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.util.List;

public class RequestMethodUtils {
    private static final Logger log = LoggerFactory.getLogger(RequestMethodUtils.class);

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public static void setAuthenticated(HttpServletRequest request, HttpServletResponse response, UserDto userDto) {
        UserDetails userDetails = (UserDetails) userDto;

        // Set security context
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails == null ? List.of() : userDetails.getAuthorities()
        );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Add header
        try {
            response.setHeader("username", userDto.getUsername());
        } catch (Exception e) {
            log.error("[setAuthentication]", e);
        }
    }
}

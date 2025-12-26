package com.example.lib.auth.filter;

import com.example.lib.auth.dto.TokenResponseDto;
import com.example.lib.auth.dto.UserDto;
import com.example.lib.auth.service.TokenService;
import com.example.lib.auth.util.RequestMethodUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class JwtTokenFilter extends OncePerRequestFilter {
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Kiểm tra đã đăng nhập chưa?
        if (RequestMethodUtils.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Chưa đăng nhâp:
        UserDto userDto = null;
        // Lấy authorization header
        final String headers = request.getHeader(HttpHeaders.AUTHORIZATION);
        // Lấy access token
        String accessToken = headers.split(" ")[1].trim();

        // Lấy request url, method
        String requestUrl = request.getRequestURI();
        String requestMethod = request.getMethod();

        // Nếu URL là permit (public API) -> ko chặn
        if (tokenService.isPermitUrl(requestUrl)) {
            TokenResponseDto tokenResponseDto = null;
            // Nếu có access token -> gọi sang auth service để xác thực (api/v1/auth/token)
            if (!accessToken.isEmpty()) {
                try {
                    tokenResponseDto = tokenService.getToken(accessToken, null, null);
                } catch (Exception e) {
                    log.error("[tokenResponseDto]", e);
                }
            }

            // Nếu access token hợp lệ -> setAuthenticated
            if (tokenResponseDto != null && tokenResponseDto.isSuccess()) {
                userDto = tokenResponseDto.getInfo();
                RequestMethodUtils.setAuthenticated(request, response, userDto);
            }
        } else {
            // Nếu ko lấy đc access token -> trả 401 cho FE xử lý
            if (accessToken.isEmpty()) {
                log.error("[Unauthorize]");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                filterChain.doFilter(request, response);
                return;
            }

            TokenResponseDto tokenResponseDto = null;
            // Gọi sang auth service để xác thực (api/v1/auth/token)
            try {
                tokenResponseDto = tokenService.getToken(accessToken, null, null);
            } catch (Exception e) {
                log.error("[tokenResponseDto]", e);
            }

            // Nếu access token ko hợp lệ -> trả 403 cho FE xử lý
            if (tokenResponseDto == null || (!tokenResponseDto.isSuccess() && !tokenResponseDto.isForbidden())) {
                log.error("[Invalid token]");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                filterChain.doFilter(request, response);
                return;
            }

            // setAuthenticated
            userDto = tokenResponseDto.getInfo();
            RequestMethodUtils.setAuthenticated(request, response, userDto);

        }

        filterChain.doFilter(request, response);
    }
}

package com.example.lib.auth.service;

import com.example.lib.auth.dto.TokenRequestDto;
import com.example.lib.auth.dto.TokenResponseDto;
import com.example.lib.auth.util.AuthUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
@Slf4j
public class TokenService {
    private final RestTemplate restTemplate;

    private final AuthUtils authUtils;

    private final AntPathMatcher antPathMatcher;

    // Hàm này gọi sang auth service: api/v1/auth/token để verify token
    public TokenResponseDto getToken(String accessToken, String requestUrl, String requestMethod) throws Exception {
        // Tạo request body gửi sang auth service
        TokenRequestDto tokenRequestDto = TokenRequestDto.builder()
                .accessToken(accessToken)
                .requestUrl(requestUrl)
                .requestMethod(requestMethod)
                .build();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Gói body + header -> HttpEntity
        HttpEntity<TokenRequestDto> request = new HttpEntity<>(tokenRequestDto, headers);

        // Gửi POST sang auth service (String url, Object request, Class<T> responseType)
        return restTemplate.postForEntity(authUtils.getTokenUrl(), request, TokenResponseDto.class).getBody();
    }

    public boolean isPermitUrl(String requestUrl) {
        for (String url : authUtils.getPermitUrls()) {
            if (antPathMatcher.match(url, requestUrl)) {
                return true;
            }
        }

        return false;
    }
}

package com.example.lib.auth.util;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class AuthUtils {
    @Value("${auth.token-url}")
    private String tokenUrl;

    @Value("${auth.login-url}")
    private String loginUrl;

    @Value("${auth.permit-urls}")
    private String[] permitUrls;
}

package com.yassinecharit.budget.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret          Base64-encoded HMAC key (at least 256 bits)
 * @param expirationHours token lifetime
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationHours) {
}

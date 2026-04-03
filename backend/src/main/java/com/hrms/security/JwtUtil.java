package com.hrms.security;

import com.hrms.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final JwtProperties props;

    public JwtUtil(JwtProperties props) {
        this.props = props;
    }

    private SecretKey key() {
        byte[] bytes = props.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, 32));
            bytes = padded;
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(Long userId, String username, String roleCode) {
        long exp = System.currentTimeMillis() + props.getExpireHours() * 3600_000L;
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", roleCode)
                .expiration(new Date(exp))
                .issuedAt(new Date())
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

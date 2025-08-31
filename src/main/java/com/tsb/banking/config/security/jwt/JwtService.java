package com.tsb.banking.config.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.tsb.banking.exception.BadRequestException;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final Key key;
    private final long accessMillis;
    private final long refreshMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.accessMinutes}") long accessMinutes,
                      @Value("${app.jwt.refreshDays}") long refreshDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.accessMillis = accessMinutes * 60 * 1000;
        this.refreshMillis = refreshDays * 24 * 60 * 60 * 1000L;
    }

    public String generateAccessToken(UserDetails user) {
        String roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.joining(","));
        return Jwts.builder()
                .setSubject(user.getUsername())
                .addClaims(Map.of("roles", roles))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UserDetails user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshMillis))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String jwt) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(jwt).getBody().getSubject();
    }

    // com.tsb.banking.config.security.jwt.JwtService
public String generateResetToken(String email) {
    Instant now = Instant.now();
    return Jwts.builder()
        .setSubject(email)
        .claim("typ", "RESET")
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(now.plusSeconds(10 * 60))) // 10 minutes
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
}

public String validateAndExtractResetEmail(String token) {
    var claims = Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
    if (!"RESET".equals(claims.get("typ"))) throw new BadRequestException("Invalid reset token");
    if (claims.getExpiration().before(new Date())) throw new BadRequestException("Expired reset token");
    return claims.getSubject(); // email
}

}

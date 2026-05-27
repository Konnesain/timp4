package timp.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(String username) {
        return generateToken(username, "access", accessExpiration);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, "refresh", refreshExpiration);
    }

    private String generateToken(String username, String type, long expiration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("type", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isAccessTokenValid(String token) {
        return isTokenValid(token, "access");
    }

    public boolean isRefreshTokenValid(String token) {
        return isTokenValid(token, "refresh");
    }

    private boolean isTokenValid(String token, String expectedType) {
        try {
            Claims claims = getClaims(token);
            return expectedType.equals(claims.get("type", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

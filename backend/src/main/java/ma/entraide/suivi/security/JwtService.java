package ma.entraide.suivi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ma.entraide.suivi.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration ttl;

    public JwtService(AppProperties props) {
        if (props.jwtSecret() == null || props.jwtSecret().length() < 32)
            throw new IllegalStateException("APP_JWT_SECRET doit contenir au moins 32 caractères");
        this.key = Keys.hmacShaKeyFor(props.jwtSecret().getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofHours(props.jwtTtlHours());
    }

    public String issue(Long userId) {
        Date now = new Date();
        return Jwts.builder().subject(String.valueOf(userId)).issuedAt(now)
                .expiration(new Date(now.getTime() + ttl.toMillis())).signWith(key).compact();
    }

    public Optional<Long> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(Long.parseLong(c.getSubject()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public Duration ttl() { return ttl; }
}

package timp.service;

import timp.model.RefreshToken;
import timp.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final long refreshExpiration;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        this.repository = repository;
        this.refreshExpiration = refreshExpiration;
    }

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        String token = UUID.randomUUID().toString();
        Instant expiry = Instant.now().plusMillis(refreshExpiration);
        RefreshToken rt = new RefreshToken(token, username, expiry);
        return repository.save(rt);
    }

    @Transactional
    public RefreshToken verifyAndRotate(String rawToken) {
        RefreshToken rt = repository.findByToken(rawToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (Instant.now().isAfter(rt.getExpiryDate())) {
            throw new RuntimeException("Refresh token expired");
        }

        repository.delete(rt);

        return createRefreshToken(rt.getUsername());
    }

    @Transactional
    public void revokeAllForUser(String username) {
        repository.deleteByUsername(username);
    }

    @Transactional
    public void revokeByToken(String rawToken) {
        repository.findByToken(rawToken).ifPresent(rt ->
                repository.deleteByUsername(rt.getUsername())
        );
    }
}

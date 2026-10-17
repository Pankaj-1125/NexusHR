package com.nexushr.service;

import com.nexushr.entity.RefreshToken;
import com.nexushr.entity.User;
import com.nexushr.repository.RefreshTokenRepository;
import com.nexushr.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${nexushr.jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public String createRefreshToken(UserDetails userDetails) {

        if (userDetails == null ||
                userDetails.getUsername() == null ||
                userDetails.getUsername().isBlank()) {

            throw new IllegalArgumentException(
                    "User details are required."
            );
        }

        if (!userDetails.isEnabled()) {
            throw new IllegalArgumentException(
                    "User account is disabled."
            );
        }

        revokeAllForUser(userDetails.getUsername());

        byte[] randomBytes = new byte[64];

        secureRandom.nextBytes(randomBytes);

        String rawToken =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(randomBytes);

        String tokenHash =
                hashToken(rawToken);

        Instant expiresAt =
                Instant.now()
                        .plusMillis(refreshTokenExpiration);

        RefreshToken refreshToken =
                new RefreshToken(
                        tokenHash,
                        userDetails.getUsername(),
                        expiresAt
                );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(
            String rawToken
    ) {

        if (rawToken == null ||
                rawToken.isBlank()) {

            throw new IllegalArgumentException(
                    "Refresh token is required."
            );
        }

        String tokenHash =
                hashToken(rawToken);

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid refresh token."
                                )
                        );

        if (refreshToken.isRevoked()) {

            throw new IllegalArgumentException(
                    "Refresh token has been revoked."
            );
        }

        if (refreshToken.getExpiresAt()
                .isBefore(Instant.now())) {

            throw new IllegalArgumentException(
                    "Refresh token has expired."
            );
        }

        User user =
                userRepository
                        .findByUsername(
                                refreshToken.getUsername()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User account not found."
                                )
                        );

        if (!user.isEnabled()) {

            throw new IllegalArgumentException(
                    "User account is disabled."
            );
        }

        return refreshToken;
    }

    @Transactional
    public void revokeToken(String rawToken) {

        if (rawToken == null ||
                rawToken.isBlank()) {

            return;
        }

        String tokenHash =
                hashToken(rawToken);

        refreshTokenRepository
                .findByTokenHash(tokenHash)
                .ifPresent(refreshToken -> {

                    refreshToken.setRevoked(true);

                    refreshTokenRepository
                            .save(refreshToken);
                });
    }

    @Transactional
    public void revokeAllForUser(
            String username
    ) {

        if (username == null ||
                username.isBlank()) {

            return;
        }

        List<RefreshToken> tokens =
                refreshTokenRepository
                        .findByUsernameAndRevokedFalse(
                                username
                        );

        tokens.forEach(token ->
                token.setRevoked(true)
        );

        if (!tokens.isEmpty()) {

            refreshTokenRepository
                    .saveAll(tokens);
        }
    }

    public String hashToken(String rawToken) {

        if (rawToken == null ||
                rawToken.isBlank()) {

            throw new IllegalArgumentException(
                    "Refresh token is required."
            );
        }

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            rawToken.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder hexString =
                    new StringBuilder();

            for (byte b : hash) {

                String hex =
                        Integer.toHexString(
                                0xff & b
                        );

                if (hex.length() == 1) {
                    hexString.append('0');
                }

                hexString.append(hex);
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available.",
                    exception
            );
        }
    }
}
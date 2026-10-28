package com.ktpm.parking.infrastructure.security;

import com.ktpm.parking.business.port.AccessTokenIssuer;
import com.ktpm.parking.business.service.AuthenticationResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
public final class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtAccessTokenIssuer(JwtEncoder jwtEncoder, SecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AuthenticationResult issue(String subject, List<String> roles) {
        Instant now = clock.instant();
        long expiresIn = properties.jwtExpirationSeconds();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("automated-parking-system")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .subject(subject)
                .claim("roles", List.copyOf(roles))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return AuthenticationResult.bearer(token, expiresIn);
    }
}

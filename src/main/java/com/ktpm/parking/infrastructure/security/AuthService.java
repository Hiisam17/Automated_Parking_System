package com.ktpm.parking.infrastructure.security;

import com.ktpm.parking.api.dto.LoginRequest;
import com.ktpm.parking.api.dto.LoginResponse;
import com.ktpm.parking.business.domain.AdminAccount;
import com.ktpm.parking.business.repository.AdminAccountRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service xử lý nghiệp vụ xác thực đăng nhập và phát hành JWT Token cho quản trị viên (Admin).
 * Thuộc phạm vi phụ trách của Member 3 (API & Security).
 */
@Service
public class AuthService {

    private final AdminAccountRepository adminAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final SecurityProperties securityProperties;

    public AuthService(
            AdminAccountRepository adminAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            SecurityProperties securityProperties
    ) {
        this.adminAccountRepository = adminAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.securityProperties = securityProperties;
    }

    /**
     * Xác thực thông tin đăng nhập của Admin và tạo JWT Bearer Token.
     *
     * @param request dữ liệu yêu cầu đăng nhập chứa username và password
     * @return LoginResponse chứa accessToken, tokenType và thời gian hiệu lực
     * @throws BadCredentialsException khi tài khoản không tồn tại, bị vô hiệu hóa hoặc mật khẩu không khớp
     */
    public LoginResponse login(LoginRequest request) {
        // 1. Tìm tài khoản Admin theo username và kiểm tra tài khoản có đang hoạt động (enabled) không
        AdminAccount admin = adminAccountRepository.findByUsername(request.username())
                .filter(AdminAccount::enabled)
                .orElseThrow(() -> new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng."));

        // 2. So khớp mật khẩu nhập vào với mật khẩu đã mã hóa bằng BCrypt trong cơ sở dữ liệu
        if (!passwordEncoder.matches(request.password(), admin.passwordHash())) {
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        Instant now = Instant.now();
        long expiresIn = securityProperties.jwtExpirationSeconds();

        // 3. Xây dựng payload (Claims) cho JWT Token:
        //    - issuer: tên dịch vụ phát hành token
        //    - subject: tên đăng nhập của Admin
        //    - roles: danh sách quyền hạn (ADMIN) dùng để phân quyền trên các Admin API
        //    - iat, exp: thời điểm phát hành và thời điểm hết hạn của token
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("automated-parking-system")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .subject(admin.username())
                .claim("roles", List.of("ADMIN"))
                .build();

        // 4. Ký token bằng thuật toán HMAC-SHA256 với secret key đã cấu hình
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();

        // 5. Trả về LoginResponse theo đúng chuẩn OpenAPI
        return LoginResponse.of(token, expiresIn);
    }
}

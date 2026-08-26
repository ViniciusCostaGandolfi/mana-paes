package vgandolfi.dev.mana_paes.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.ForgotPasswordRequest;
import vgandolfi.dev.mana_paes.application.dto.request.LoginRequest;
import vgandolfi.dev.mana_paes.application.dto.request.RefreshTokenRequest;
import vgandolfi.dev.mana_paes.application.dto.request.RegisterRequest;
import vgandolfi.dev.mana_paes.application.dto.request.ResetPasswordRequest;
import vgandolfi.dev.mana_paes.application.dto.response.LoginResponse;
import vgandolfi.dev.mana_paes.application.dto.response.UserResponse;
import vgandolfi.dev.mana_paes.config.AppProperties;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.model.PasswordResetToken;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.PasswordResetTokenRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.infrastructure.security.JwtService;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       NotificationService notificationService,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.appProperties = appProperties;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        log.info("auth_login_success userId={} tenantId={}", user.getId(), user.getTenant().getId());
        return buildLoginResponse(user);
    }

    /**
     * Cria um novo tenant e o primeiro usuário como ROLE_ADMIN (owner do tenant).
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessException("Já existe uma conta com este e-mail");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setActive(true);
        Tenant savedTenant = tenantRepository.save(tenant);

        User user = new User();
        user.setTenant(savedTenant);
        user.setName(request.name());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setWhatsappNumber(request.whatsappNumber());
        user.setRole(UserRole.ROLE_ADMIN);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        log.info("auth_register_success userId={} tenantId={}", savedUser.getId(), savedTenant.getId());
        return buildLoginResponse(savedUser);
    }

    @Transactional
    public LoginResponse refresh(RefreshTokenRequest request) {
        UUID userId = jwtService.parseRefreshToken(request.refreshToken());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido"));
        if (!user.isActive()) {
            throw new BadCredentialsException("Usuário inativo");
        }
        return buildLoginResponse(user);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email())).orElse(null);
        if (user == null) {
            // não revela a existência da conta
            log.info("auth_forgot_password email_not_found");
            return;
        }

        String token = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(token);
        resetToken.setExpiryDate(Instant.now().plus(Duration.ofHours(1)));
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);

        log.info("auth_forgot_password token_created userId={} expiresAt={}", user.getId(), resetToken.getExpiryDate());
        notificationService.sendForgotPasswordEmail(user, token, resetToken.getExpiryDate());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new BusinessException("Token de recuperação inválido"));
        if (resetToken.isUsed()) {
            throw new BusinessException("Token de recuperação já utilizado");
        }
        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new BusinessException("Token de recuperação expirado");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("auth_reset_password success userId={}", user.getId());
    }

    private LoginResponse buildLoginResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        return LoginResponse.of(accessToken, refreshToken, appProperties.jwt().expiration(), UserResponse.from(user));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
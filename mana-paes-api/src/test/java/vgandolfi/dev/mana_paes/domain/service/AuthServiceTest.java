package vgandolfi.dev.mana_paes.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import vgandolfi.dev.mana_paes.application.dto.request.ForgotPasswordRequest;
import vgandolfi.dev.mana_paes.application.dto.request.LoginRequest;
import vgandolfi.dev.mana_paes.application.dto.request.RegisterRequest;
import vgandolfi.dev.mana_paes.application.dto.request.ResetPasswordRequest;
import vgandolfi.dev.mana_paes.application.dto.response.LoginResponse;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private NotificationService notificationService;

    private AppProperties appProperties() {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Evolution("", ""),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false));
    }

    /**
     * Constrói o serviço explicitamente: {@code AppProperties} é um valor real
     * (não-mock) e o {@code @InjectMocks} não injetaria.
     */
    private AuthService authService() {
        return new AuthService(userRepository, tenantRepository, passwordResetTokenRepository,
                passwordEncoder, jwtService, notificationService, appProperties());
    }

    private Tenant tenant(UUID id) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        return tenant;
    }

    private User user(UUID id, String email, UserRole role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setActive(active);
        user.setTenant(tenant(UUID.randomUUID()));
        return user;
    }

    @Test
    void registerCreatesTenantAndAdmin() {
        when(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant tenant = inv.getArgument(0);
            tenant.setId(UUID.randomUUID());
            return tenant;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");

        LoginResponse response = authService().register(new RegisterRequest("Dona Padaria", "novo@example.com",
                "senha123", null, null));

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.user().role()).isEqualTo(UserRole.ROLE_ADMIN);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void registerWithDuplicateEmailThrows() {
        when(userRepository.findByEmail("duplicado@example.com"))
                .thenReturn(Optional.of(user(UUID.randomUUID(), "duplicado@example.com", UserRole.ROLE_ADMIN, true)));

        assertThatThrownBy(() -> authService().register(new RegisterRequest("X", "duplicado@example.com",
                "senha123", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma conta");
    }

    @Test
    void loginWithValidCredentialsReturnsToken() {
        User existing = user(UUID.randomUUID(), "joao@example.com", UserRole.ROLE_ADMIN, true);
        when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("senha123", existing.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(existing)).thenReturn("access");
        when(jwtService.generateRefreshToken(existing)).thenReturn("refresh");

        LoginResponse response = authService().login(new LoginRequest("Joao@Example.com ", "senha123"));

        assertThat(response.accessToken()).isEqualTo("access");
    }

    @Test
    void loginWithWrongPasswordThrows() {
        User existing = user(UUID.randomUUID(), "joao@example.com", UserRole.ROLE_ADMIN, true);
        when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("errada", existing.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService().login(new LoginRequest("joao@example.com", "errada")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginWithInactiveUserThrows() {
        User inactive = user(UUID.randomUUID(), "inativo@example.com", UserRole.ROLE_ADMIN, false);
        when(userRepository.findByEmail("inativo@example.com")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> authService().login(new LoginRequest("inativo@example.com", "senha123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginWithUnknownEmailThrows() {
        when(userRepository.findByEmail("nope@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService().login(new LoginRequest("nope@example.com", "senha123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void forgotPasswordForUnknownEmailDoesNotReveal() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        authService().forgotPassword(new ForgotPasswordRequest("ghost@example.com"));

        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void forgotPasswordCreatesResetToken() {
        User existing = user(UUID.randomUUID(), "joao@example.com", UserRole.ROLE_ADMIN, true);
        when(userRepository.findByEmail("joao@example.com")).thenReturn(Optional.of(existing));

        authService().forgotPassword(new ForgotPasswordRequest("joao@example.com"));

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        assertThat(tokenCaptor.getValue().getToken()).isNotBlank();
        assertThat(tokenCaptor.getValue().getExpiryDate()).isAfter(Instant.now());
        verify(notificationService).sendForgotPasswordEmail(any(User.class), anyString(), any(Instant.class));
    }

    @Test
    void resetPasswordWithExpiredTokenThrows() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsed(false);
        token.setExpiryDate(Instant.now().minus(1, ChronoUnit.HOURS));
        when(passwordResetTokenRepository.findByToken("expired")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService().resetPassword(new ResetPasswordRequest("expired", "nova123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void resetPasswordWithUsedTokenThrows() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUsed(true);
        token.setExpiryDate(Instant.now().plus(1, ChronoUnit.HOURS));
        when(passwordResetTokenRepository.findByToken("used")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService().resetPassword(new ResetPasswordRequest("used", "nova123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("já utilizado");
    }

    @Test
    void refreshWithInvalidTokenThrows() {
        when(jwtService.parseRefreshToken("invalid")).thenThrow(new BadCredentialsException("Refresh token inválido"));

        assertThatThrownBy(() -> authService().refresh(new vgandolfi.dev.mana_paes.application.dto.request.RefreshTokenRequest("invalid")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
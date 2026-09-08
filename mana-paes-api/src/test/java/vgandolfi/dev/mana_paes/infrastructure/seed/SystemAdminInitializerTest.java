package vgandolfi.dev.mana_paes.infrastructure.seed;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAdminInitializerTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ApplicationArguments args;

    private AppProperties appProperties(String email, String password) {
        return new AppProperties(
                new AppProperties.Jwt("test-secret-test-secret-test-secret-test-secret-1234", 3600000L, 86400000L),
                new AppProperties.Encryption("mana-paes-test-master-key-32chars!"),
                new AppProperties.Evolution("", "", 0L, "mana-paes", false),
                new AppProperties.Backend("http://localhost:8080"),
                new AppProperties.Frontend("http://localhost"),
                new AppProperties.Mail(false),
                new AppProperties.Notifications(false, 2),
                new AppProperties.Scheduler(false),
                new AppProperties.SystemAdmin(email, password));
    }

    private SystemAdminInitializer initializer(AppProperties props) {
        return new SystemAdminInitializer(userRepository, tenantRepository, passwordEncoder, props);
    }

    @Test
    void createsRootTenantAndSysAdminWhenEnvConfigured() {
        when(userRepository.findByEmail("vinicius@vgandolfi.dev")).thenReturn(Optional.empty());
        when(userRepository.existsByRole(UserRole.ROLE_SYS_ADMIN)).thenReturn(false);
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
        when(passwordEncoder.encode("admin123")).thenReturn("hash");

        initializer(appProperties("vinicius@vgandolfi.dev", "admin123")).run(args);

        verify(tenantRepository).save(any(Tenant.class));
        org.mockito.ArgumentCaptor<User> captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ROLE_SYS_ADMIN);
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void skipsWhenSysAdminAlreadyExists() {
        when(userRepository.findByEmail("vinicius@vgandolfi.dev"))
                .thenReturn(Optional.of(new User()));

        initializer(appProperties("vinicius@vgandolfi.dev", "admin123")).run(args);

        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void skipsWhenEnvNotConfigured() {
        initializer(appProperties("", "")).run(args);

        verify(userRepository, never()).findByEmail(any());
        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
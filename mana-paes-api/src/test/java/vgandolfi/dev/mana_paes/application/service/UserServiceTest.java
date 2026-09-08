package vgandolfi.dev.mana_paes.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vgandolfi.dev.mana_paes.application.dto.request.UserRequest;
import vgandolfi.dev.mana_paes.application.dto.response.UserResponse;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private User user(String email, UserRole role) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(role);
        user.setTenant(new Tenant());
        user.getTenant().setId(tenantId);
        return user;
    }

    private UserRequest adminRequest(String email, String password) {
        return new UserRequest("Novo Admin", email, null, null, UserRole.ROLE_ADMIN, password);
    }

    @Test
    void sysAdminCreatesAdminWithNewTenant() {
        when(userRepository.findByEmail("novo@example.com")).thenReturn(Optional.empty());
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant tenant = inv.getArgument(0);
            tenant.setId(UUID.randomUUID());
            return tenant;
        });
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.create(UUID.randomUUID(), UserRole.ROLE_SYS_ADMIN,
                adminRequest("novo@example.com", "senha123"));

        assertThat(response.role()).isEqualTo(UserRole.ROLE_ADMIN);
        assertThat(response.active()).isTrue();
        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().isActive()).isTrue();
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void sysAdminCannotCreateSysAdmin() {
        UserRequest request = new UserRequest("Outro", "outro@example.com", null, null,
                UserRole.ROLE_SYS_ADMIN, "senha123");

        assertThatThrownBy(() -> userService.create(UUID.randomUUID(), UserRole.ROLE_SYS_ADMIN, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SYS_ADMIN só pode criar usuários ROLE_ADMIN");
    }

    @Test
    void adminCannotCreateUsers() {
        assertThatThrownBy(() -> userService.create(UUID.randomUUID(), UserRole.ROLE_ADMIN,
                adminRequest("x@example.com", "senha123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não autorizado");
    }

    @Test
    void sysAdminCreatingAdminWithoutEmailThrows() {
        assertThatThrownBy(() -> userService.create(UUID.randomUUID(), UserRole.ROLE_SYS_ADMIN,
                adminRequest(null, "senha123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("E-mail é obrigatório");
    }

    @Test
    void sysAdminCreatingAdminWithDuplicateEmailThrows() {
        when(userRepository.findByEmail("dup@example.com"))
                .thenReturn(Optional.of(user("dup@example.com", UserRole.ROLE_ADMIN)));

        assertThatThrownBy(() -> userService.create(UUID.randomUUID(), UserRole.ROLE_SYS_ADMIN,
                adminRequest("dup@example.com", "senha123")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe uma conta");
    }

    @Test
    void updateWithEmailConflictThrows() {
        User existing = user("atual@example.com", UserRole.ROLE_ADMIN);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(existing));
        when(userRepository.existsByTenantIdAndEmail(tenantId, "outro@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(tenantId, UserRole.ROLE_SYS_ADMIN, userId,
                adminRequest("outro@example.com", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um usuário");
    }

    @Test
    void adminCannotPromoteToAdminViaUpdate() {
        assertThatThrownBy(() -> userService.update(tenantId, UserRole.ROLE_ADMIN, userId,
                adminRequest("motoboy@example.com", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ADMIN não pode atribuir ROLE_ADMIN");
    }

    @Test
    void setActiveTogglesUser() {
        User existing = user("x@example.com", UserRole.ROLE_ADMIN);
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(existing));

        UserResponse response = userService.setActive(tenantId, userId, false);

        assertThat(response.active()).isFalse();
    }

    @Test
    void getByIdNotFoundThrows() {
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(tenantId, userId))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
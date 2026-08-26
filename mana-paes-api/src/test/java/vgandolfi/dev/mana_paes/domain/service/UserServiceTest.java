package vgandolfi.dev.mana_paes.domain.service;

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

    private User user(String email) {
        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(UserRole.ROLE_REQUESTER);
        user.setTenant(new Tenant());
        user.getTenant().setId(tenantId);
        return user;
    }

    private UserRequest request(String email) {
        return new UserRequest("Funcionário", email, null, null, UserRole.ROLE_REQUESTER);
    }

    @Test
    void createWithDuplicateEmailThrows() {
        when(userRepository.existsByTenantIdAndEmail(tenantId, "dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(tenantId, request("Dup@Example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um usuário");
    }

    @Test
    void createEncodesTemporaryPasswordAndSaves() {
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        when(userRepository.existsByTenantIdAndEmail(tenantId, "novo@example.com")).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(passwordEncoder.encode(anyString())).thenReturn("temporary-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.create(tenantId, request("novo@example.com"));

        assertThat(response.role()).isEqualTo(UserRole.ROLE_REQUESTER);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("temporary-hash");
        assertThat(captor.getValue().getEmail()).isEqualTo("novo@example.com");
    }

    @Test
    void createWithMissingTenantThrows() {
        when(userRepository.existsByTenantIdAndEmail(tenantId, "novo@example.com")).thenReturn(false);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(tenantId, request("novo@example.com")))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateWithEmailConflictThrows() {
        User existing = user("atual@example.com");
        when(userRepository.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(existing));
        when(userRepository.existsByTenantIdAndEmail(tenantId, "outro@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.update(tenantId, userId, request("outro@example.com")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um usuário");
    }

    @Test
    void setActiveTogglesUser() {
        User existing = user("x@example.com");
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
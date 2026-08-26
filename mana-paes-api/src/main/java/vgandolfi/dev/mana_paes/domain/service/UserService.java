package vgandolfi.dev.mana_paes.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.application.dto.request.UserRequest;
import vgandolfi.dev.mana_paes.application.dto.response.UserResponse;
import vgandolfi.dev.mana_paes.domain.exception.BusinessException;
import vgandolfi.dev.mana_paes.domain.exception.EntityNotFoundException;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;

import java.util.UUID;

/**
 * CRUD de usuários restrito ao tenant do usuário autenticado (multi-tenant).
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(UUID tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID tenantId, UUID userId) {
        return UserResponse.from(findScoped(tenantId, userId));
    }

    @Transactional
    public UserResponse create(UUID tenantId, UserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new BusinessException("Já existe um usuário com este e-mail neste tenant");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Tenant", tenantId));

        User user = new User();
        user.setTenant(tenant);
        user.setName(request.name());
        user.setEmail(email);
        // senha temporária aleatória nunca revelada: o usuário deve usar o fluxo de recuperação de senha
        user.setPasswordHash(passwordEncoder.encode(randomTemporaryPassword()));
        user.setPhone(request.phone());
        user.setWhatsappNumber(request.whatsappNumber());
        user.setRole(request.role());
        user.setActive(true);
        User saved = userRepository.save(user);

        log.info("user_created userId={} tenantId={} role={}", saved.getId(), tenantId, saved.getRole());
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(UUID tenantId, UUID userId, UserRequest request) {
        User user = findScoped(tenantId, userId);
        String email = normalizeEmail(request.email());
        if (!user.getEmail().equals(email) && userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new BusinessException("Já existe um usuário com este e-mail neste tenant");
        }
        user.setName(request.name());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setWhatsappNumber(request.whatsappNumber());
        user.setRole(request.role());
        User saved = userRepository.save(user);

        log.info("user_updated userId={} tenantId={}", userId, tenantId);
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse setActive(UUID tenantId, UUID userId, boolean active) {
        User user = findScoped(tenantId, userId);
        user.setActive(active);
        userRepository.save(user);

        log.info("user_status_changed userId={} tenantId={} active={}", userId, tenantId, active);
        return UserResponse.from(user);
    }

    private User findScoped(UUID tenantId, UUID userId) {
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> EntityNotFoundException.of("Usuário", userId));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String randomTemporaryPassword() {
        return UUID.randomUUID().toString().replace("-", "") + "Aa1!";
    }
}
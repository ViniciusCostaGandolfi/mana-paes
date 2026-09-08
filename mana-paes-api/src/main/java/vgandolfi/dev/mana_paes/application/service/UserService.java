package vgandolfi.dev.mana_paes.application.service;

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
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;

import java.util.UUID;

/**
 * CRUD de usuários restrito ao tenant do usuário autenticado (multi-tenant).
 *
 * <p>Criação por papel: apenas o {@code SYS_ADMIN} cria usuários — um
 * {@code ADMIN} em um tenant novo (nome, e-mail e senha inicial fornecidos).
 * Nenhum outro papel cria usuários e ninguém atribui {@code SYS_ADMIN} por
 * este endpoint.</p>
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository,
                       PasswordEncoder passwordEncoder) {
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
    public UserResponse create(UUID tenantId, UserRole actorRole, UserRequest request) {
        if (actorRole != UserRole.ROLE_SYS_ADMIN) {
            throw new BusinessException("Papel não autorizado a criar usuários");
        }
        if (request.role() != UserRole.ROLE_ADMIN) {
            throw new BusinessException("SYS_ADMIN só pode criar usuários ROLE_ADMIN");
        }
        String email = normalizeEmail(request.email());
        if (!hasText(email)) {
            throw new BusinessException("E-mail é obrigatório para criar um ADMIN");
        }
        if (!hasText(request.password())) {
            throw new BusinessException("Senha inicial é obrigatória para criar um ADMIN");
        }
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
        User saved = userRepository.save(user);

        log.info("user_created userId={} tenantId={} role=ROLE_ADMIN by=sys_admin", saved.getId(), savedTenant.getId());
        return UserResponse.from(saved);
    }

    @Transactional
    public UserResponse update(UUID tenantId, UserRole actorRole, UUID userId, UserRequest request) {
        if (request.role() == UserRole.ROLE_SYS_ADMIN) {
            throw new BusinessException("Não é possível atribuir ROLE_SYS_ADMIN por este endpoint");
        }
        if (actorRole == UserRole.ROLE_ADMIN && request.role() == UserRole.ROLE_ADMIN) {
            throw new BusinessException("ADMIN não pode atribuir ROLE_ADMIN");
        }
        User user = findScoped(tenantId, userId);
        String email = normalizeEmail(request.email());
        if (hasText(email) && !email.equals(user.getEmail())
                && userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw new BusinessException("Já existe um usuário com este e-mail neste tenant");
        }
        user.setName(request.name());
        user.setEmail(email);
        user.setPhone(request.phone());
        user.setWhatsappNumber(request.whatsappNumber());
        user.setRole(request.role());
        User saved = userRepository.save(user);

        log.info("user_updated userId={} tenantId={} role={}", userId, tenantId, saved.getRole());
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
package vgandolfi.dev.mana_paes.infrastructure.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;

/**
 * Cria o primeiro {@code SYS_ADMIN} do sistema a partir do env
 * ({@code app.system-admin.email/password}), em TODOS os perfis (inclusive
 * dev e test). Idempotente: se já existir um usuário com o e-mail do env — ou
 * qualquer {@code SYS_ADMIN} — nada é criado.
 *
 * <p>Se o env estiver vazio (ex.: prod ainda não configurada), apenas loga um
 * warn e segue — o sistema continua sendo configurado por outros meios.</p>
 *
 * <p>Roda antes do {@link DevDataSeeder} ({@code @Order(HIGHEST_PRECEDENCE)}),
 * que então reutiliza o tenant raiz criado aqui.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SystemAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SystemAdminInitializer.class);

    static final String ROOT_TENANT_NAME = "Mana Paes";

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties appProperties;

    public SystemAdminInitializer(UserRepository userRepository,
                                  TenantRepository tenantRepository,
                                  PasswordEncoder passwordEncoder,
                                  AppProperties appProperties) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.appProperties = appProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.SystemAdmin sysAdmin = appProperties.systemAdmin();
        if (sysAdmin == null || !hasText(sysAdmin.email()) || !hasText(sysAdmin.password())) {
            log.warn("system_admin_skip reason=env_not_configured");
            return;
        }
        String email = sysAdmin.email().trim().toLowerCase();
        if (userRepository.findByEmail(email).isPresent() || userRepository.existsByRole(UserRole.ROLE_SYS_ADMIN)) {
            log.info("system_admin_skip reason=already_exists email={}", email);
            return;
        }

        Tenant tenant = new Tenant();
        tenant.setName(ROOT_TENANT_NAME);
        tenant.setActive(true);
        Tenant savedTenant = tenantRepository.save(tenant);

        User user = new User();
        user.setTenant(savedTenant);
        user.setName("Vinicius Gandolfi");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(sysAdmin.password()));
        user.setRole(UserRole.ROLE_SYS_ADMIN);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        log.info("system_admin_created userId={} tenantId={} email={} (SYS_ADMIN)", savedUser.getId(),
                savedTenant.getId(), email);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
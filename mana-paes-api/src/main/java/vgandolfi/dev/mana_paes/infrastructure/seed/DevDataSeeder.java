package vgandolfi.dev.mana_paes.infrastructure.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import vgandolfi.dev.mana_paes.application.dto.request.OrderItemRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderRequest;
import vgandolfi.dev.mana_paes.application.dto.request.OrderStatusUpdateRequest;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.enums.OrderStatus;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.domain.service.OrderService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seed de dados para DESENVOLVIMENTO (perfil {@code dev}).
 *
 * <p>Cria, de forma idempotente, um tenant de demonstração com o admin do
 * projeto (Vinicius), funcionários, produtos típicos de padaria, pedidos em
 * status variados e a {@code NotificationConfig} do tenant.</p>
 *
 * <p><b>Escopo:</b> anotado com {@code @Profile("dev")} — nunca executa nos
 * perfis default (H2) nem {@code test} (os testes de integração criam seus
 * próprios dados). <b>Idempotência:</b> se o admin ({@value #ADMIN_EMAIL}) já
 * existir, nada é duplicado. <b>Atomicidade:</b> roda dentro de um
 * {@link TransactionTemplate} — em caso de falha, nada é persistido.</p>
 *
 * <p>As senhas abaixo são credenciais de DESENVOLVIMENTO (não secretas),
 * exigidas pela especificação do seed e registradas em log para conveniência
 * local. Em produção nunca existem dados seed.</p>
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    static final String ADMIN_EMAIL = "vinicius@vgandolfi.dev";
    static final String ADMIN_PASSWORD = "admin123";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final NotificationConfigRepository configRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderService orderService;
    private final TransactionTemplate transactionTemplate;

    public DevDataSeeder(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         ProductRepository productRepository,
                         NotificationConfigRepository configRepository,
                         PasswordEncoder passwordEncoder,
                         OrderService orderService,
                         TransactionTemplate transactionTemplate) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.configRepository = configRepository;
        this.passwordEncoder = passwordEncoder;
        this.orderService = orderService;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            transactionTemplate.executeWithoutResult(status -> seed());
        } catch (Exception ex) {
            log.error("seed_failed — dados de dev não foram criados (rollback aplicado)", ex);
        }
    }

    private void seed() {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            log.info("seed_skip reason=admin_exists email={}", ADMIN_EMAIL);
            return;
        }

        Tenant tenant = new Tenant();
        tenant.setName("Padaria do Vinicius");
        tenant.setDocument("12.345.678/0001-90");
        tenant.setPhone("5511999999999");
        tenant.setAddress("Rua das Flores, 123 — São Paulo/SP");
        tenant.setActive(true);
        Tenant savedTenant = tenantRepository.save(tenant);

        User admin = createUser(savedTenant, "Vinicius Gandolfi", ADMIN_EMAIL, ADMIN_PASSWORD,
                UserRole.ROLE_ADMIN, "5511999999999");
        User daiana = createUser(savedTenant, "Daiana Silva", "daiana@vgandolfi.dev", "daiana123",
                UserRole.ROLE_REQUESTER, "5511988887777");
        User carlos = createUser(savedTenant, "Carlos Pereira", "carlos@vgandolfi.dev", "carlos123",
                UserRole.ROLE_REQUESTER, "5511977776666");
        User marcos = createUser(savedTenant, "Marcos Souza", "marcos@vgandolfi.dev", "marcos123",
                UserRole.ROLE_PRODUCTION, "5511966665555");

        createNotificationConfig(savedTenant, admin);

        List<Product> products = createProducts(savedTenant);

        List<UUID> orderIds = createOrders(savedTenant, admin, daiana, carlos, products);

        log.info("seed_complete tenantId={} admin={} requester={} production=1 products={} orders={} "
                        + "adminPassword={} (DEV ONLY)",
                savedTenant.getId(), admin.getEmail(), "daiana@vgandolfi.dev, carlos@vgandolfi.dev",
                products.size(), orderIds.size(), ADMIN_PASSWORD);
    }

    private User createUser(Tenant tenant, String name, String email, String password,
                            UserRole role, String whatsappNumber) {
        User user = new User();
        user.setTenant(tenant);
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPhone(whatsappNumber);
        user.setWhatsappNumber(whatsappNumber);
        user.setRole(role);
        user.setActive(true);
        User saved = userRepository.save(user);
        // credenciais de DEV (não-secretas) exigidas pela especificação do seed
        log.info("seed_user_created userId={} name={} email={} role={} whatsapp={} password={} (DEV ONLY)",
                saved.getId(), name, email, role, whatsappNumber, password);
        return saved;
    }

    private void createNotificationConfig(Tenant tenant, User admin) {
        NotificationConfig config = new NotificationConfig();
        config.setTenant(tenant);
        config.setAdminWhatsappNumber(admin.getWhatsappNumber());
        config.setAdminEmail(admin.getEmail());
        config.setDailyReportTime(LocalTime.of(18, 0));
        config.setWhatsappEnabled(true);
        config.setEmailEnabled(true);
        config.setEvolutionApiInstanceName("mana-paes");
        configRepository.save(config);
        log.info("seed_notification_config_created tenantId={} adminEmail={} instance=mana-paes",
                tenant.getId(), admin.getEmail());
    }

    private List<Product> createProducts(Tenant tenant) {
        List<Product> products = new ArrayList<>();
        products.add(createProduct(tenant, "Pão Francês", "Pão francês tradicional", "0.50", UnitMeasure.UN));
        products.add(createProduct(tenant, "Pão de Forma", "Pão de forma fatiado", "8.50", UnitMeasure.UN));
        products.add(createProduct(tenant, "Biscoito", "Biscoito caseiro", "19.90", UnitMeasure.KG));
        products.add(createProduct(tenant, "Rosca", "Rosca doce com cobertura", "12.00", UnitMeasure.UN));
        products.add(createProduct(tenant, "Pão Integral", "Pão integral com grãos", "0.90", UnitMeasure.UN));
        products.add(createProduct(tenant, "Pão de Leite", "Pão de leite macio", "4.50", UnitMeasure.UN));
        return products;
    }

    private Product createProduct(Tenant tenant, String name, String description, String price, UnitMeasure unit) {
        Product product = new Product();
        product.setTenant(tenant);
        product.setName(name);
        product.setDescription(description);
        product.setUnitPrice(new BigDecimal(price));
        product.setUnitMeasure(unit);
        product.setActive(true);
        return productRepository.save(product);
    }

    private List<UUID> createOrders(Tenant tenant, User admin, User daiana, User carlos,
                                    List<Product> products) {
        Product paoFrances = product(products, "Pão Francês");
        Product paoForma = product(products, "Pão de Forma");
        Product biscoito = product(products, "Biscoito");
        Product rosca = product(products, "Rosca");
        Product paoIntegral = product(products, "Pão Integral");
        Product paoLeite = product(products, "Pão de Leite");

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<UUID> orderIds = new ArrayList<>();
        // hoje: 2 PENDING + 1 IN_PRODUCTION + 1 DELIVERED; amanhã: 1 PENDING + 1 READY
        orderIds.add(createOrder(tenant, admin, daiana, today, OrderStatus.PENDING,
                List.of(item(paoFrances, "20"), item(paoLeite, "2"))));
        orderIds.add(createOrder(tenant, admin, daiana, today, OrderStatus.IN_PRODUCTION,
                List.of(item(paoIntegral, "10"), item(rosca, "1"))));
        orderIds.add(createOrder(tenant, admin, carlos, today, OrderStatus.DELIVERED,
                List.of(item(paoFrances, "30"), item(paoLeite, "3"))));
        orderIds.add(createOrder(tenant, admin, daiana, tomorrow, OrderStatus.PENDING,
                List.of(item(rosca, "2"), item(paoForma, "1"))));
        orderIds.add(createOrder(tenant, admin, carlos, tomorrow, OrderStatus.READY,
                List.of(item(paoForma, "1"), item(biscoito, "2"))));
        return orderIds;
    }

    /**
     * Cria o pedido via {@link OrderService} (mesmo cálculo de total, snapshot
     * de preço e histórico do fluxo real) e, para status != PENDING, percorre a
     * cadeia de transições válidas adicionando {@code OrderStatusHistory}.
     */
    private UUID createOrder(Tenant tenant, User admin, User requester, LocalDate deliveryDate,
                             OrderStatus finalStatus, List<OrderItemRequest> items) {
        OrderRequest request = new OrderRequest(deliveryDate, items, null);
        OrderResponse created = orderService.create(tenant.getId(), requester.getId(),
                UserRole.ROLE_REQUESTER, request);

        for (OrderStatus next : pathTo(finalStatus)) {
            orderService.updateStatus(tenant.getId(), admin.getId(), created.id(),
                    new OrderStatusUpdateRequest(next, "Seed de desenvolvimento"));
        }

        log.info("seed_order_created orderId={} requester={} date={} status={} total={} items={}",
                created.id(), requester.getEmail(), deliveryDate, finalStatus,
                created.totalAmount(), items.size());
        return created.id();
    }

    private List<OrderStatus> pathTo(OrderStatus finalStatus) {
        return switch (finalStatus) {
            case IN_PRODUCTION -> List.of(OrderStatus.IN_PRODUCTION);
            case READY -> List.of(OrderStatus.IN_PRODUCTION, OrderStatus.READY);
            case DELIVERED -> List.of(OrderStatus.IN_PRODUCTION, OrderStatus.READY, OrderStatus.DELIVERED);
            default -> List.of();
        };
    }

    private OrderItemRequest item(Product product, String quantity) {
        return new OrderItemRequest(product.getId(), new BigDecimal(quantity));
    }

    private Product product(List<Product> products, String name) {
        return products.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Produto do seed não encontrado: " + name));
    }
}
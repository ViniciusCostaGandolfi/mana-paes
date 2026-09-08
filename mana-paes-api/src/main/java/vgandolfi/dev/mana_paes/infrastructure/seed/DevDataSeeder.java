package vgandolfi.dev.mana_paes.infrastructure.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import vgandolfi.dev.mana_paes.application.dto.request.OrderItemRequest;
import vgandolfi.dev.mana_paes.application.dto.request.PublicOrderRequest;
import vgandolfi.dev.mana_paes.application.dto.request.SelectionRequest;
import vgandolfi.dev.mana_paes.application.dto.response.OrderResponse;
import vgandolfi.dev.mana_paes.application.service.OrderService;
import vgandolfi.dev.mana_paes.domain.model.NotificationConfig;
import vgandolfi.dev.mana_paes.domain.model.Product;
import vgandolfi.dev.mana_paes.domain.model.ProductOption;
import vgandolfi.dev.mana_paes.domain.model.ProductOptionGroup;
import vgandolfi.dev.mana_paes.domain.model.ScheduledReminder;
import vgandolfi.dev.mana_paes.domain.model.Tenant;
import vgandolfi.dev.mana_paes.domain.model.User;
import vgandolfi.dev.mana_paes.domain.model.Vendor;
import vgandolfi.dev.mana_paes.domain.model.enums.ConnectionState;
import vgandolfi.dev.mana_paes.domain.model.enums.UnitMeasure;
import vgandolfi.dev.mana_paes.domain.model.enums.UserRole;
import vgandolfi.dev.mana_paes.domain.repository.NotificationConfigRepository;
import vgandolfi.dev.mana_paes.domain.repository.ProductRepository;
import vgandolfi.dev.mana_paes.domain.repository.ScheduledReminderRepository;
import vgandolfi.dev.mana_paes.domain.repository.TenantRepository;
import vgandolfi.dev.mana_paes.domain.repository.UserRepository;
import vgandolfi.dev.mana_paes.domain.repository.VendorRepository;
import vgandolfi.dev.mana_paes.infrastructure.config.AppProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seed de dados para DESENVOLVIMENTO (perfil {@code dev}): fábrica de pães.
 *
 * <p>O {@code SYS_ADMIN} é criado pelo {@link SystemAdminInitializer} (todos
 * os perfis). Este seed complementa o tenant raiz com o {@code ADMIN} (Breno),
 * vendedores, produtos da fábrica (com grupos de opções), pedidos por turno
 * (incluindo o pedido de quinta do Everton da planilha), a
 * {@code NotificationConfig} e um lembrete de exemplo.</p>
 *
 * <p><b>Idempotência:</b> se o admin ({@value #BRENO_EMAIL}) já existir, nada
 * é duplicado. <b>Atomicidade:</b> roda dentro de um
 * {@link TransactionTemplate} — em caso de falha, nada é persistido.</p>
 */
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    static final String BRENO_EMAIL = "breno@vgandolfi.dev";
    private static final String BRENO_PASSWORD = "breno123";
    private static final String BRENO_WHATSAPP = "5511988880000";

    private static final String EVERTON_PHONE = "5511900000001";
    private static final String DAIANA_PHONE = "5511900000002";
    private static final String CARLOS_PHONE = "5511900000003";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final NotificationConfigRepository configRepository;
    private final VendorRepository vendorRepository;
    private final ScheduledReminderRepository reminderRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrderService orderService;
    private final TransactionTemplate transactionTemplate;

    public DevDataSeeder(TenantRepository tenantRepository,
                         UserRepository userRepository,
                         ProductRepository productRepository,
                         NotificationConfigRepository configRepository,
                         VendorRepository vendorRepository,
                         ScheduledReminderRepository reminderRepository,
                         PasswordEncoder passwordEncoder,
                         OrderService orderService,
                         TransactionTemplate transactionTemplate) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.configRepository = configRepository;
        this.vendorRepository = vendorRepository;
        this.reminderRepository = reminderRepository;
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
        if (userRepository.findByEmail(BRENO_EMAIL).isPresent()) {
            log.info("seed_skip reason=admin_exists email={}", BRENO_EMAIL);
            return;
        }

        // tenant raiz criado pelo SystemAdminInitializer (contém o SYS_ADMIN)
        Tenant tenant = rootTenantBySysAdmin();

        User breno = createUser(tenant, "Breno Silva", BRENO_EMAIL, BRENO_PASSWORD,
                UserRole.ROLE_ADMIN, BRENO_WHATSAPP);
        createVendor(tenant, "Everton", EVERTON_PHONE);
        createVendor(tenant, "Daiana", DAIANA_PHONE);
        createVendor(tenant, "Carlos", CARLOS_PHONE);

        createNotificationConfig(tenant, breno);

        List<Product> products = createProducts(tenant);
        int orders = createOrders(products);
        createReminder(tenant);

        log.info("seed_complete tenantId={} admin={} vendors=3 products={} orders={} (DEV ONLY)",
                tenant.getId(), breno.getEmail(), products.size(), orders);
    }

    private Tenant rootTenantBySysAdmin() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.ROLE_SYS_ADMIN)
                .findFirst()
                .map(User::getTenant)
                .orElseThrow(() -> new IllegalStateException("SYS_ADMIN do seed não encontrado"));
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
        log.info("seed_user_created userId={} name={} email={} role={} whatsapp={} (DEV ONLY)",
                saved.getId(), name, email, role, whatsappNumber);
        return saved;
    }

    private void createVendor(Tenant tenant, String name, String phone) {
        Vendor vendor = new Vendor();
        vendor.setTenant(tenant);
        vendor.setName(name);
        vendor.setPhone(phone);
        vendor.setWhatsappNumber(phone);
        vendor.setActive(true);
        Vendor saved = vendorRepository.save(vendor);
        log.info("seed_vendor_created vendorId={} name={} phone={}", saved.getId(), name, phone);
    }

    private void createNotificationConfig(Tenant tenant, User admin) {
        NotificationConfig config = new NotificationConfig();
        config.setTenant(tenant);
        config.setAdminWhatsappNumber(admin.getWhatsappNumber());
        config.setAdminEmail(admin.getEmail());
        config.setDailyReportTime(LocalTime.of(18, 0));
        config.setWhatsappEnabled(true);
        config.setEmailEnabled(false);
        configRepository.save(config);
        log.info("seed_notification_config_created tenantId={} adminWhatsapp={}",
                tenant.getId(), admin.getWhatsappNumber());
    }

    private List<Product> createProducts(Tenant tenant) {
        List<Product> products = new ArrayList<>();
        products.add(plainProduct(tenant, "Milho", "Bolo de milho", "16.80"));
        products.add(plainProduct(tenant, "Leite", "Bolo de leite", "18.00"));
        products.add(plainProduct(tenant, "Creme", "Bolo de creme", "18.00"));
        products.add(plainProduct(tenant, "Coco", "Bolo de coco", "18.00"));
        products.add(productWithOptions(tenant, "Pão de sal", "Pão de sal francês", "0.55",
                group("Tamanho",
                        option("Pequeno", "0.00"),
                        option("Grande", "0.20"))));
        products.add(plainProduct(tenant, "Pres/queijo", "Pão com presunto e queijo", "4.00"));
        products.add(plainProduct(tenant, "Frango/catupiry", "Pão com frango e catupiry", "4.00"));
        products.add(plainProduct(tenant, "Frios", "Pão com frios", "4.00"));
        products.add(plainProduct(tenant, "Hamburguer", "Pão com hambúrguer", "4.00"));
        products.add(plainProduct(tenant, "Pão de queijo", "Pão de queijo", "2.00"));
        products.add(productWithOptions(tenant, "Rosca recheada", "Rosca doce recheada", "3.00",
                group("Recheio",
                        option("Coco", "0.00"),
                        option("Creme", "0.00"),
                        option("Doce de leite", "0.00"))));
        products.add(plainProduct(tenant, "Rosca sem recheio", "Rosca doce sem recheio", "1.50"));
        return products;
    }

    private Product plainProduct(Tenant tenant, String name, String description, String price) {
        Product product = new Product();
        product.setTenant(tenant);
        product.setName(name);
        product.setDescription(description);
        product.setUnitPrice(new BigDecimal(price));
        product.setUnitMeasure(UnitMeasure.UN);
        product.setActive(true);
        return productRepository.save(product);
    }

    private Product productWithOptions(Tenant tenant, String name, String description, String price,
                                       ProductOptionGroup group) {
        Product product = new Product();
        product.setTenant(tenant);
        product.setName(name);
        product.setDescription(description);
        product.setUnitPrice(new BigDecimal(price));
        product.setUnitMeasure(UnitMeasure.UN);
        product.setActive(true);
        product.addOptionGroup(group);
        return productRepository.save(product);
    }

    private ProductOptionGroup group(String name, ProductOption... options) {
        ProductOptionGroup group = new ProductOptionGroup();
        group.setName(name);
        group.setRequired(false);
        for (ProductOption option : options) {
            group.addOption(option);
        }
        return group;
    }

    private ProductOption option(String name, String adjustment) {
        ProductOption option = new ProductOption();
        option.setName(name);
        option.setPriceAdjustment(new BigDecimal(adjustment));
        option.setActive(true);
        return option;
    }

    /**
     * Pedidos de exemplo criados pelo fluxo público (link mágico): o pedido de
     * quinta do Everton da planilha (total R$ 343,40), um pedido da Daiana com
     * turnos variados + opção com preço adicional, e um do Carlos para amanhã.
     */
    private int createOrders(List<Product> products) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        OrderResponse everton = orderService.createPublicOrder(new PublicOrderRequest(
                EVERTON_PHONE, "Everton", today, List.of(
                        item(products, "Milho", "3", "0", "0", null),
                        item(products, "Leite", "3", "0", "0", null),
                        item(products, "Creme", "2", "0", "0", null),
                        item(products, "Coco", "2", "0", "0", null),
                        item(products, "Pão de sal", "50", "50", "0", null),
                        item(products, "Pres/queijo", "6", "0", "0", null),
                        item(products, "Frango/catupiry", "4", "0", "0", null),
                        item(products, "Frios", "4", "0", "0", null),
                        item(products, "Hamburguer", "4", "0", "0", null),
                        item(products, "Rosca recheada", "12", "0", "0", null),
                        item(products, "Pão de queijo", "2", "0", "0", null))));

        // Daiana: Pão de sal GRANDE (opção +0,20) com turnos variados + Milho à tarde + Pão de queijo
        SelectionRequest grande = selection(products, "Pão de sal", "Tamanho", "Grande");
        OrderResponse daiana = orderService.createPublicOrder(new PublicOrderRequest(
                DAIANA_PHONE, "Daiana", today, List.of(
                        item(products, "Pão de sal", "20", "30", "10", grande),
                        item(products, "Milho", "0", "4", "0", null),
                        item(products, "Pão de queijo", "10", "0", "0", null))));

        // Carlos: pedido para amanhã (Rosca recheada de creme)
        SelectionRequest creme = selection(products, "Rosca recheada", "Recheio", "Creme");
        OrderResponse carlos = orderService.createPublicOrder(new PublicOrderRequest(
                CARLOS_PHONE, "Carlos", tomorrow, List.of(
                        item(products, "Pão de sal", "30", "0", "0", null),
                        item(products, "Rosca recheada", "5", "0", "0", creme))));

        log.info("seed_orders_created everton={} daiana={} carlos={}", everton.totalAmount(),
                daiana.totalAmount(), carlos.totalAmount());
        return 3;
    }

    private void createReminder(Tenant tenant) {
        ScheduledReminder reminder = new ScheduledReminder();
        reminder.setTenant(tenant);
        reminder.setName("Lembrete de pedidos");
        reminder.setMessage("Hora de fazer os pedidos");
        reminder.setReminderTime(LocalTime.of(9, 0));
        reminder.setEnabled(true);
        reminder.setWhatsappEnabled(true);
        reminder.setEmailEnabled(false);
        reminder.setDeliveryDayOffset(1);
        reminderRepository.save(reminder);
        log.info("seed_reminder_created tenantId={} time=09:00 offset=1", tenant.getId());
    }

    private OrderItemRequest item(List<Product> products, String name, String manha, String tarde,
                                  String noite, SelectionRequest selection) {
        Product product = find(products, name);
        return new OrderItemRequest(product.getId(),
                new BigDecimal(manha), new BigDecimal(tarde), new BigDecimal(noite),
                selection == null ? null : List.of(selection));
    }

    private SelectionRequest selection(List<Product> products, String productName, String groupName,
                                       String optionName) {
        Product product = find(products, productName);
        ProductOptionGroup group = product.getOptionGroups().stream()
                .filter(g -> g.getName().equals(groupName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Grupo " + groupName + " não encontrado em " + productName));
        ProductOption option = group.getOptions().stream()
                .filter(o -> o.getName().equals(optionName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Opção " + optionName + " não encontrada em " + productName));
        return new SelectionRequest(group.getId(), option.getId());
    }

    private Product find(List<Product> products, String name) {
        return products.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Produto do seed não encontrado: " + name));
    }
}
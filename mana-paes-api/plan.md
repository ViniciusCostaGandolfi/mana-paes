# Plano Backend — Mana Paes

As skills do repositório (`springboot-security`, `springboot-tdd`, `springboot-patterns`, `jpa-patterns`) definem as convenções a seguir: JWT stateless, arquitetura em camadas, DTOs records com Bean Validation, testes primeiro, Testcontainers, Flyway, lazy loading e auditoria com JPA.

---

## 1. Tecnologias e Versões

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.0.7 |
| Segurança | Spring Security 6.x + JWT |
| Persistência | Spring Data JPA (Hibernate) |
| Banco de dados | PostgreSQL (produção), H2 (dev/testes) |
| Migrations | Flyway |
| Cache | Redis |
| Mensageria | RabbitMQ ou Redis Pub/Sub |
| E-mail | Spring Mail (SMTP) |
| Agendamento | Spring Scheduling (`@Scheduled`) |
| Documentação | SpringDoc OpenAPI |
| Build | Maven + GraalVM Native Image |

---

## 2. Estrutura de Pacotes

```
com.vgandolfi.manapaes
├── ManaPaesApplication.java
├── config
│   ├── SecurityConfig.java
│   ├── JwtConfig.java
│   ├── CorsConfig.java
│   ├── RedisConfig.java
│   ├── RabbitConfig.java
│   ├── AsyncConfig.java
│   ├── JpaConfig.java
│   └── FlywayConfig.java
├── domain
│   ├── model/
│   │   ├── Tenant.java
│   │   ├── User.java
│   │   ├── Product.java
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── OrderStatusHistory.java
│   │   ├── DailyReport.java
│   │   ├── DailyReportItem.java
│   │   ├── NotificationConfig.java
│   │   ├── NotificationLog.java
│   │   └── PasswordResetToken.java
│   ├── repository/
│   └── service/
├── application
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── mapper/
│   ├── usecase/
│   └── event/
│       └── OrderCreatedEvent.java
├── infrastructure
│   ├── persistence/
│   ├── security/
│   │   ├── JwtAuthFilter.java
│   │   ├── JwtService.java
│   │   └── UserDetailsServiceImpl.java
│   ├── notification/
│   │   ├── EvolutionApiClient.java
│   │   ├── EmailNotificationAdapter.java
│   │   ├── WhatsAppNotificationAdapter.java
│   │   └── NotificationPublisher.java
│   ├── scheduler/
│   │   └── DailyReportScheduler.java
│   └── webhook/
│       └── EvolutionWebhookHandler.java
└── api
    ├── controller/
    ├── exception/
    │   └── GlobalExceptionHandler.java
    └── mapper/
```

---

## 3. Entidades JPA

Todas as entidades usam:

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(indexes = { ... })
```

Com `@CreatedDate`, `@LastModifiedDate` e IDs `UUID` gerados por `UUIDGenerator`.

### `Tenant`

```java
UUID id
String name
String document (CNPJ)
String phone
String address
boolean active
Instant createdAt
Instant updatedAt
```

**Relacionamentos:** `1:N` com `User`, `Product`, `Order`; `1:1` com `NotificationConfig`.

### `User`

```java
UUID id
Tenant tenant
String name
String email (unique por tenant)
String passwordHash
String phone
String whatsappNumber
UserRole role (ADMIN, REQUESTER, PRODUCTION)
boolean active
Instant createdAt
Instant updatedAt
```

**Relacionamentos:** `N:1` com `Tenant`; `1:N` com `Order` (como solicitante) e `OrderStatusHistory` (quem alterou).

### `Product`

```java
UUID id
Tenant tenant
String name
String description
BigDecimal unitPrice
UnitMeasure unitMeasure (UN, KG)
boolean active
Instant createdAt
Instant updatedAt
```

**Relacionamentos:** `N:1` com `Tenant`; `1:N` com `OrderItem`.

### `Order`

```java
UUID id
Tenant tenant
User requester
Instant createdAt
LocalDate deliveryDate
OrderStatus status (PENDING, IN_PRODUCTION, READY, DELIVERED, CANCELLED)
BigDecimal totalAmount
List<OrderItem> items
Instant updatedAt
```

**Relacionamentos:** `N:1` com `Tenant` e `User`; `1:N` com `OrderItem` e `OrderStatusHistory`.

### `OrderItem`

```java
UUID id
Order order
Product product
BigDecimal quantity
BigDecimal unitPrice // snapshot do preço no momento do pedido
BigDecimal subtotal
```

**Relacionamentos:** `N:1` com `Order` e `Product`.

### `OrderStatusHistory`

```java
UUID id
Order order
OrderStatus previousStatus
OrderStatus newStatus
User changedBy
Instant changedAt
String reason
```

**Relacionamentos:** `N:1` com `Order` e `User`.

### `DailyReport`

```java
UUID id
Tenant tenant
LocalDate reportDate
BigDecimal totalAmount
int totalOrders
boolean sent
Instant generatedAt
List<DailyReportItem> items
```

### `DailyReportItem`

```java
UUID id
DailyReport report
Product product
BigDecimal totalQuantity
BigDecimal totalAmount
```

### `NotificationConfig`

```java
UUID id
Tenant tenant
String adminWhatsappNumber
String adminEmail
LocalTime dailyReportTime // ex: 18:00
boolean whatsappEnabled
boolean emailEnabled
String evolutionApiInstanceName
String evolutionApiKey
```

**Relacionamentos:** `1:1` com `Tenant`.

### `NotificationLog`

```java
UUID id
Order order // nullable
NotificationChannel channel (WHATSAPP, EMAIL)
NotificationType type (ORDER_CONFIRMATION_REQUESTER, NEW_ORDER_ADMIN_ALERT, DAILY_REPORT)
String recipient
NotificationStatus status (PENDING, SENT, FAILED)
String content
String errorMessage
int retryCount
Instant sentAt
Instant createdAt
```

**Relacionamentos:** `N:1` com `Order` (opcional).

### `PasswordResetToken`

```java
UUID id
User user
String token
Instant expiryDate
boolean used
```

**Relacionamentos:** `N:1` com `User`.

---

## 4. Enums

```java
enum UserRole { ROLE_ADMIN, ROLE_REQUESTER, ROLE_PRODUCTION }
enum OrderStatus { PENDING, IN_PRODUCTION, READY, DELIVERED, CANCELLED }
enum UnitMeasure { UN, KG }
enum NotificationChannel { WHATSAPP, EMAIL }
enum NotificationType { ORDER_CONFIRMATION_REQUESTER, NEW_ORDER_ADMIN_ALERT, DAILY_REPORT }
enum NotificationStatus { PENDING, SENT, FAILED }
```

---

## 5. DTOs (records + Bean Validation)

### Auth

```java
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password
) {}

public record LoginResponse(
    String accessToken,
    String tokenType,
    Long expiresIn,
    UserResponse user
) {}

public record RegisterRequest(...) {}
public record ForgotPasswordRequest(@NotBlank @Email String email) {}
public record ResetPasswordRequest(@NotBlank @Size(min = 6) String password) {}
```

### User

```java
public record UserRequest(
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Email String email,
    String phone,
    String whatsappNumber,
    @NotNull UserRole role
) {}

public record UserResponse(UUID id, String name, String email, UserRole role, boolean active) {}
```

### Product

```java
public record ProductRequest(
    @NotBlank @Size(max = 200) String name,
    String description,
    @NotNull @DecimalMin("0.01") BigDecimal unitPrice,
    @NotNull UnitMeasure unitMeasure
) {}

public record ProductResponse(UUID id, String name, BigDecimal unitPrice, UnitMeasure unitMeasure, boolean active) {}
```

### Order

```java
public record OrderItemRequest(
    @NotNull UUID productId,
    @NotNull @DecimalMin("0.01") BigDecimal quantity
) {}

public record OrderRequest(
    @NotNull @FutureOrPresent LocalDate deliveryDate,
    @NotEmpty List<OrderItemRequest> items
) {}

public record OrderStatusUpdateRequest(
    @NotNull OrderStatus status,
    @Size(max = 500) String reason
) {}
```

### Report

```java
public record DailyProductionReportResponse(LocalDate date, List<DailyReportItemResponse> items, BigDecimal totalAmount) {}
public record DailyFinancialReportResponse(LocalDate date, BigDecimal totalAmount, int totalOrders) {}
```

---

## 6. Repositórios

```java
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByRequesterId(UUID requesterId);
    List<Order> findByTenantIdAndDeliveryDate(UUID tenantId, LocalDate deliveryDate);
    List<Order> findByTenantIdAndStatusAndDeliveryDate(UUID tenantId, OrderStatus status, LocalDate deliveryDate);

    @Query("select o from Order o left join fetch o.items where o.id = :id")
    Optional<Order> findWithItems(@Param("id") UUID id);
}

public interface ProductRepository extends JpaRepository<Product, UUID> {
    List<Product> findByTenantIdAndActiveTrue(UUID tenantId);
}

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {
    Optional<DailyReport> findByTenantIdAndReportDate(UUID tenantId, LocalDate reportDate);
}
```

---

## 7. Camada de Serviço

| Serviço | Responsabilidade |
|---|---|
| `AuthService` | login, registro, refresh token, recuperação de senha |
| `UserService` | CRUD de usuários |
| `ProductService` | CRUD de produtos |
| `OrderService` | criação, cálculo de total, atualização de status |
| `ReportService` | geração de relatórios consolidados |
| `NotificationService` | orquestra envio de notificações |
| `NotificationConfigService` | configurações de notificação |
| `TenantService` | gerenciamento do tenant |

Todas as queries de leitura usam `@Transactional(readOnly = true)`. Injeção por construtor. Exceções tratadas centralmente no `GlobalExceptionHandler`.

---

## 8. Segurança (skill `springboot-security`)

### JwtAuthFilter

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Authentication auth = jwtService.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(request, response);
    }
}
```

### SecurityConfig

```java
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter,
                                           CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/api/v1/webhooks/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

### Proteção de endpoints

```java
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/users")
public List<UserResponse> listUsers() { ... }

@PreAuthorize("hasAnyRole('ADMIN', 'PRODUCTION')")
@PatchMapping("/orders/{id}/status")
public OrderResponse updateStatus(...) { ... }

@PreAuthorize("hasRole('REQUESTER')")
@PostMapping("/orders")
public OrderResponse createOrder(...) { ... }
```

### Boas práticas aplicadas

- Senhas com `BCryptPasswordEncoder(12)`
- CSRF desabilitado (API stateless com Bearer tokens)
- CORS restrito por origem, nunca `*` em produção
- Secrets via variáveis de ambiente (`${DB_PASSWORD}`, `JWT_SECRET`, etc.)
- Rate limiting com Bucket4j em endpoints sensíveis
- Headers de segurança (CSP, frameOptions, referrerPolicy)
- Logs estruturados sem PII

---

## 9. Estratégia de Testes (skill `springboot-tdd`)

### Pirâmide de testes

1. **Unitários** — JUnit 5 + Mockito para services e mappers
2. **Web layer** — `@WebMvcTest` + MockMvc para controllers
3. **Integração** — `@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers (Postgres, Redis, RabbitMQ)
4. **Persistência** — `@DataJpaTest` + Testcontainers

### Cobertura

- JaCoCo com meta mínima de 80%
- Comando: `mvn verify`

### Exemplo de teste unitário

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;
    @InjectMocks OrderService orderService;

    @Test
    void calculatesTotalOnCreate() {
        // arrange
        // act
        // assert
    }
}
```

### Convenções

- Arrange-Act-Assert
- AssertJ (`assertThat`) para legibilidade
- `jsonPath` para respostas JSON
- `assertThatThrownBy` para exceções
- Test data builders para entidades

---

## 10. Migrations Flyway

```
src/main/resources/db/migration/
├── V1__create_tenants.sql
├── V2__create_users.sql
├── V3__create_products.sql
├── V4__create_orders.sql
├── V5__create_order_items.sql
├── V6__create_order_status_history.sql
├── V7__create_daily_reports.sql
├── V8__create_notification_config.sql
├── V9__create_notification_logs.sql
└── V10__create_password_reset_tokens.sql
```

`spring.jpa.hibernate.ddl-auto=validate` — nunca `update` em produção.

---

## 11. Integrações

### WhatsApp — EvolutionApiClient

```java
@Component
public class EvolutionApiClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String apiKey;

    public void sendText(String instance, String number, String text) {
        // POST /message/sendText/{instance}
        // com retry exponencial
    }
}
```

### E-mail — EmailNotificationAdapter

```java
@Component
public class EmailNotificationAdapter {
    private final JavaMailSender mailSender;

    public void sendHtmlEmail(String to, String subject, String htmlBody) { ... }
}
```

### Fila assíncrona

```java
public interface NotificationPublisher {
    void publish(OrderCreatedEvent event);
}
```

Implementações: RabbitMQ ou Redis Pub/Sub. Consumer grava `NotificationLog` e faz retry/DLQ em caso de falha.

### Webhook handler

```java
@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {
    @PostMapping("/evolution-api")
    public ResponseEntity<Void> handleEvolutionWebhook(@RequestBody EvolutionWebhookPayload payload) { ... }
}
```

Eventos tratados: `CONNECTION_UPDATE`, `QRCODE_UPDATED`, `MESSAGES_UPSERT`.

---

## 12. Agendamento

```java
@Component
public class DailyReportScheduler {
    @Scheduled(cron = "0 0 18 * * *")
    public void sendDailyReports() {
        // para cada tenant ativo, verificar horário configurado e enviar
    }
}
```

---

## 13. Configurações application.yml

```yaml
spring:
  application:
    name: mana-paes
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:manapaes}
    username: ${DB_USER:manapaes}
    password: ${DB_PASS:manapaes}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        jdbc:
          lob:
            non_contextual_creation: true
  flyway:
    enabled: true
  mail:
    host: ${SMTP_HOST}
    port: ${SMTP_PORT}
    username: ${SMTP_USER}
    password: ${SMTP_PASS}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
  rabbitmq:
    host: ${RABBIT_HOST:localhost}
    username: ${RABBIT_USER}
    password: ${RABBIT_PASS}

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000
  evolution:
    url: ${EVOLUTION_API_URL}
    global-api-key: ${EVOLUTION_API_KEY}
  frontend:
    url: ${FRONTEND_URL}
```

---

## 14. Checklist de Implementação

### Fase 1 — Fundação

- [ ] Configurar `pom.xml` (Web, Data JPA, Security, Mail, Flyway, Postgres, Redis, RabbitMQ, Testcontainers, JaCoCo)
- [ ] Configurar perfis `dev`, `test`, `prod`
- [ ] Criar entidades e enums
- [ ] Criar migrations Flyway V1-V10
- [ ] Configurar JPA Auditing

### Fase 2 — Autenticação

- [ ] Implementar `JwtService`, `JwtAuthFilter`, `SecurityConfig`
- [ ] Implementar `AuthController` (login, register, forgot/reset password)
- [ ] Implementar `UserService` e `UserController`

### Fase 3 — Domínio Principal

- [ ] CRUD de produtos
- [ ] Criação e gestão de pedidos
- [ ] Atualização de status com histórico
- [ ] Relatórios diários consolidados

### Fase 4 — Notificações

- [ ] Integração com Evolution API
- [ ] Integração com SMTP
- [ ] Fila assíncrona para notificações
- [ ] Webhook da Evolution API

### Fase 5 — Agendamento e Finalização

- [ ] Agendador do relatório diário
- [ ] Testes unitários, web e integração
- [ ] JaCoCo coverage ≥ 80%
- [ ] Build GraalVM native image
- [ ] Docker Compose completo

---

## 15. Decisões Pendentes

1. **Multi-tenant:** uma única padaria ou várias? *(Se for uma, remover `Tenant` do MVP.)*
2. **JWT:** só access token ou access + refresh token?
3. **Fila assíncrona:** RabbitMQ ou Redis Pub/Sub?
4. **Horário do relatório:** fixo (18h) ou configurável por tenant?
5. **Relatório por e-mail:** HTML no corpo basta ou precisa de PDF anexo?
6. **E-mail de cobrança/mensalidade:** entra no MVP ou fica para depois?
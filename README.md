# Mana Paes — Sistema de Pedidos e Produção para Padaria

Sistema para padarias gerenciarem pedidos de produção feitos por padeiros/revendedores. Os solicitantes registram pedidos (ex.: 1.000 pães, 3 biscoitos, 4 roscas) pelo aplicativo; a administração acompanha os pedidos e a produção recebe a consolidação diária de quantidades e valores.

Projeto integrador da faculdade — stack: **Spring Boot 4.0.7 (Native Image)** no backend, **React + TypeScript + Vite + DaisyUI (PWA)** no frontend, com notificações via **WhatsApp (Evolution API)** e **e-mail (SMTP)**.

---

## Sumário

- [Visão Geral](#visão-geral)
- [Stack Tecnológica](#stack-tecnológica)
- [Requisitos Funcionais](#requisitos-funcionais)
  - [RF01 — Autenticação e Perfis](#rf01--autenticação-e-perfis)
  - [RF02 — Catálogo de Produtos](#rf02--catálogo-de-produtos)
  - [RF03 — Pedidos](#rf03--pedidos)
  - [RF04 — Consolidação de Produção e Relatórios](#rf04--consolidação-de-produção-e-relatórios)
  - [RF05 — Notificações via WhatsApp (Evolution API)](#rf05--notificações-via-whatsapp-evolution-api)
  - [RF06 — Notificações por E-mail (SMTP)](#rf06--notificações-por-e-mail-smtp)
  - [RF07 — Configurações do Sistema](#rf07--configurações-do-sistema)
- [Requisitos Não-Funcionais](#requisitos-não-funcionais)
- [Modelagem de Dados](#modelagem-de-dados)
- [Endpoints Principais](#endpoints-principais)
- [Arquitetura](#arquitetura)
- [Riscos e Considerações](#riscos-e-considerações)
- [Próximos Passos](#próximos-passos)

---

## Visão Geral

**Contexto:** um cliente (Breno, administrador da padaria) solicitou um software para que os padeiros façam pedidos de produção via aplicativo. Cada solicitante (ex.: Daiana) informa quantidades de itens (pães, biscoitos, roscas), o pedido chega ao administrativo, que o consolida para a produção, e o sistema gera um relatório diário de quantidades e valores.

**Perfis de usuário:**
- **Solicitante** — padeiro/revendedor que registra pedidos.
- **Administração** — Breno/gestão, acompanha pedidos, emite relatórios e gerencia configurações.
- **Produção** — responsável pela preparação dos itens, atualiza status.

---

## Stack Tecnológica

### Backend
| Tecnologia | Uso |
|---|---|
| Java 25 + Spring Boot 4.0.7 | API REST |
| GraalVM Native Image | Compilação nativa (build `-Pnative`) |
| Spring Security + JWT | Autenticação e autorização por perfil |
| Spring Data JPA (Hibernate) | Persistência |
| PostgreSQL + Flyway | Banco de dados e migrations |
| H2 | Banco em memória para testes/desenvolvimento |
| Spring Mail (SMTP) | Envio de e-mails |
| Spring Scheduling (`@Scheduled`) | Relatório diário agendado |
| Redis | Cache (catálogo e relatórios consolidados) |
| RabbitMQ ou Redis Pub/Sub | Fila assíncrona para notificações com retry/DLQ |
| SpringDoc OpenAPI | Documentação Swagger |

### Frontend
| Tecnologia | Uso |
|---|---|
| React + TypeScript | Interface |
| Vite | Build e dev server |
| Tailwind CSS + DaisyUI | Estilização e componentes |
| `vite-plugin-pwa` (Workbox) | PWA: manifest, service worker, offline, instalação |
| React Router | Rotas |
| TanStack Query | Data fetching e cache (opcional) |
| Axios | HTTP client |

### Infraestrutura
- Docker + Docker Compose
- HTTPS obrigatório em produção (requisito do PWA)
- Evolution API auto-hospedada (Docker) para WhatsApp

---

## Requisitos Funcionais

### RF01 — Autenticação e Perfis

- **RF01.1:** Autenticação via JWT com três perfis de acesso: `SOLICITANTE`, `ADMIN` e `PRODUCAO`.
- **RF01.2:** Cadastro e gerenciamento de solicitantes (nome, telefone, e-mail, WhatsApp).
- **RF01.3:** Controle de acesso por endpoint conforme o perfil.
- **RF01.4:** Fluxo de recuperação de senha por e-mail (SMTP).

### RF02 — Catálogo de Produtos

- **RF02.1:** CRUD de produtos (ex.: Pão francês, Biscoito, Rosca) com nome, unidade de medida (`UN`, `KG`), preço unitário e status (ativo/inativo).
- **RF02.2:** Consulta do catálogo de produtos ativos pelos solicitantes.
- **RF02.3** *(opcional)*: Controle de estoque mínimo e alerta de produtos.

### RF03 — Pedidos

- **RF03.1:** O solicitante registra um pedido com itens, quantidades individuais e data/horário previsto de entrega/retirada.
- **RF03.2:** Cálculo automático do valor total com base nos preços unitários vigentes no momento da criação.
- **RF03.3:** Alteração de status do pedido (`PENDENTE`, `EM_PRODUCAO`, `PRONTO`, `ENTREGUE`, `CANCELADO`) pelos perfis Administração e Produção.
- **RF03.4:** Visualização de pedidos por solicitante e visão administrativa de todos os pedidos.
- **RF03.5** *(opcional)*: Duplicar pedido anterior (pedidos recorrentes).

### RF04 — Consolidação de Produção e Relatórios

- **RF04.1:** Agrupar e somar as quantidades de cada produto necessárias para a produção em uma data específica.
- **RF04.2:** Relatório diário de produção (itens + quantidades).
- **RF04.3:** Relatório diário financeiro (faturamento total e por produto).
- **RF04.4:** Envio manual do relatório por WhatsApp e e-mail.

### RF05 — Notificações via WhatsApp (Evolution API)

- **RF05.1:** Notificação automática via WhatsApp para o administrador a cada novo pedido criado.
- **RF05.2:** Mensagem de confirmação para o solicitante com resumo do pedido e valor total.
- **RF05.3:** Envio do relatório diário consolidado para o WhatsApp do administrador em horário programado.
- **RF05.4:** Webhook para escutar atualizações de status de envio e status da conexão com a Evolution API (`CONNECTION_UPDATE`, `QRCODE_UPDATED`).
- **RF05.5:** Painel administrativo com status da conexão WhatsApp e botão de reconexão/teste.

### RF06 — Notificações por E-mail (SMTP)

- **RF06.1:** E-mail transacional de confirmação de pedido para o solicitante e a administração.
- **RF06.2:** Relatório diário consolidado em anexo (PDF/HTML) por e-mail para o administrador.
- **RF06.3:** E-mail de recuperação de senha.
- **RF06.4** *(opcional)*: E-mails de cobrança e alertas de mensalidade do modelo SaaS.

### RF07 — Configurações do Sistema

- **RF07.1:** Configuração dos dados da padaria, telefone/e-mail do administrador e horário do relatório diário.
- **RF07.2:** Habilitar/desabilitar envio por WhatsApp e e-mail.

---

## Requisitos Não-Funcionais

| Categoria | Requisito |
|---|---|
| **Arquitetura** | API RESTful separada do frontend, documentada com OpenAPI/Swagger |
| **Banco de Dados** | PostgreSQL (produção) e H2 (testes/desenvolvimento) |
| **Mensageria** | Fila assíncrona para notificações (RabbitMQ ou Redis Pub/Sub) com retry e DLQ |
| **Cache** | Redis para catálogo de produtos e relatórios consolidados do dia |
| **Segurança** | Senhas com BCrypt, endpoints protegidos por JWT, HTTPS em produção |
| **PWA** | Manifest, service worker, cache offline e prompt de instalação |
| **Deploy** | Backend containerizado (Docker); frontend em hosting estático com HTTPS |
| **Native Image** | Build GraalVM nativo funcional com validação contínua de compatibilidade de bibliotecas |

---

## Modelagem de Dados

Entidades core:

- **User** — `id`, `name`, `email`, `password_hash`, `role` (`ADMIN`, `SOLICITANTE`, `PRODUCAO`), `phone`, `whatsapp_number`.
- **Product** — `id`, `name`, `unit_price`, `unit_measure` (`UN`, `KG`), `active`.
- **Order** — `id`, `client_id`, `created_at`, `delivery_date`, `status`, `total_amount`.
- **OrderItem** — `id`, `order_id`, `product_id`, `quantity`, `unit_price`.
- **NotificationLog** — `id`, `order_id`, `recipient`, `channel` (`WHATSAPP`, `EMAIL`), `status` (`SENT`, `FAILED`, `PENDING`), `error_message`, `sent_at`.
- **NotificationConfig** — `id`, `admin_whatsapp_number`, `admin_email`, `send_daily_report_time`, `enable_whatsapp`, `enable_email`.
- **Tenant/Bakery** *(preparado para SaaS/multi-tenant)* — `id`, `name`, `subscription_active`.

---

## Endpoints Principais

### Autenticação
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

### Produtos
- `GET /api/v1/products` (catálogo ativo)
- `POST /api/v1/products` *(admin)*
- `PUT /api/v1/products/{id}` *(admin)*
- `DELETE /api/v1/products/{id}` *(admin)*

### Pedidos
- `POST /api/v1/orders`
- `GET /api/v1/orders` (listagem por perfil)
- `GET /api/v1/orders/{id}`
- `PATCH /api/v1/orders/{id}/status` *(admin/produção)*

### Relatórios
- `GET /api/v1/reports/daily/production?date=YYYY-MM-DD`
- `GET /api/v1/reports/daily/financial?date=YYYY-MM-DD`
- `POST /api/v1/notifications/reports/daily/send` *(disparo manual)*

### Notificações / Integrações
- `POST /api/v1/webhooks/evolution-api` (callbacks da Evolution API)
- `POST /api/v1/notifications/whatsapp/test`
- `GET /api/v1/notifications/whatsapp/status`
- `POST /api/v1/notifications/whatsapp/reconnect`

---

## Arquitetura

```
┌──────────────┐      HTTPS      ┌──────────────────────┐
│  Frontend    │ ──────────────► │   Backend Spring     │
│  React PWA   │                 │   Boot (Native)      │
│  (Vite +     │                 │                      │
│   DaisyUI)   │ ◄────────────── │  Auth (JWT)          │
└──────────────┘      REST/JSON  │  Pedidos/Relatórios  │
                                 │  Notificações        │
                                 └──────────┬───────────┘
                                            │
                    ┌───────────────────────┼───────────────────────┐
                    ▼                       ▼                       ▼
             ┌─────────────┐        ┌──────────────┐        ┌──────────────┐
             │ PostgreSQL  │        │   Redis      │        │   RabbitMQ   │
             │ (dados)     │        │ (cache/fila) │        │  (fila/retry)│
             └─────────────┘        └──────────────┘        └──────────────┘
                    │                       │                       │
                    └───────────────────────┴───────────────────────┘
                                            │
                          ┌─────────────────┴──────────────────┐
                          ▼                                    ▼
                 ┌─────────────────┐                  ┌─────────────────┐
                 │  Evolution API  │                  │  SMTP Server    │
                 │  (WhatsApp)     │                  │  (e-mails)      │
                 └─────────────────┘                  └─────────────────┘
```

**Fluxo de notificação assíncrona:** ao criar um pedido, a API persiste o pedido e enfileira eventos de notificação (WhatsApp + e-mail). Um consumidor processa a fila, dispara mensagens na Evolution API/SMTP e registra o resultado em `NotificationLog`, com retry e DLQ em caso de falha.

---

## Riscos e Considerações

1. **Spring Native pode complicar o projeto integrador.** Build lento, dificuldade com bibliotecas de terceiros e necessidade de testar em native image desde cedo. **Recomendação:** desenvolver primeiro como Spring Boot normal e converter para native no final (build `-Pnative`).

2. **Evolution API com Baileys não é 100% estável.** Depende do WhatsApp Web, pode desconectar e exigir leitura de QR code. Para o projeto, usar Baileys (grátis); para produção real, avaliar WhatsApp Cloud API (oficial da Meta).

3. **PWA não substitui app nativo.** No iOS Safari não há prompt automático de instalação — o usuário adiciona à tela inicial manualmente.

4. **Relatório diário agendado.** Com uma única instância do backend, `@Scheduled` do Spring é suficiente. Com múltiplas instâncias, usar Quartz com JDBC JobStore ou schedlock.

5. **Modelo SaaS / mensalidade.** A transcrição menciona cobrança de mensalidade. Para o MVP acadêmico, simplificar como configuração de "assinatura ativa" sem gateway de pagamento.

6. **GraalVM e bibliotecas de terceiros.** Bibliotecas precisam de *reachability metadata*. Spring Data JPA, Spring Security + JWT, Redis, RabbitMQ e Spring Mail têm suporte nativo; validar as demais.

---

## Próximos Passos

- [ ] Validar escopo e responder perguntas em aberto (perfis, horário do relatório, multi-tenant, hospedagem)
- [ ] Modelagem detalhada do banco de dados (DER)
- [ ] Definição da estrutura de pastas do backend e frontend
- [ ] Backlog priorizado (MVP vs extras)
- [ ] Implementação do MVP
- [ ] Build nativo (GraalVM) e validação de compatibilidade
- [ ] Deploy e testes com HTTPS (PWA)
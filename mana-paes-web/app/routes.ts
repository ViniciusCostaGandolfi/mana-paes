import { type RouteConfig, index, layout, route } from "@react-router/dev/routes";

export default [
  // Rotas públicas (fora do layout autenticado)
  index("routes/landing.tsx"),
  route("login", "routes/login.tsx"),
  route("pedido", "routes/pedido.tsx"),
  route("forgot-password", "routes/forgot-password.tsx"),
  route("reset-password", "routes/reset-password.tsx"),

  // Layout autenticado (AppLayout)
  layout("routes/_layout.tsx", [
    route("dashboard", "routes/dashboard.tsx"),

    route("orders", "routes/orders/index.tsx"),
    route("orders/new", "routes/orders/new.tsx"),
    route("orders/:id", "routes/orders/$id.tsx"),

    // Sub-layout restrito a administradores (SYS_ADMIN e ADMIN)
    layout("routes/_admin-layout.tsx", [
      route("products", "routes/products/index.tsx"),
      route("products/new", "routes/products/new.tsx"),
      route("products/:id/edit", "routes/products/edit.$id.tsx"),
      route("vendors", "routes/vendors/index.tsx"),
      route("vendors/new", "routes/vendors/new.tsx"),
      route("vendors/:id/edit", "routes/vendors/edit.$id.tsx"),
      route("users", "routes/users/index.tsx"),
      route("users/new", "routes/users/new.tsx"),
      route("users/:id/edit", "routes/users/edit.$id.tsx"),
      route("reports/production", "routes/reports/production.tsx"),
      route("reports/financial", "routes/reports/financial.tsx"),
      route("settings/notifications", "routes/settings/notifications.tsx"),
      route("settings/whatsapp", "routes/settings/whatsapp.tsx"),
      route("settings/reminders", "routes/settings/reminders.tsx"),
      route("ajuda", "routes/ajuda.tsx"),
    ]),
  ]),
] satisfies RouteConfig;
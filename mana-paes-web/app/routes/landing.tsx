import { useState } from "react";
import { Link, Navigate } from "react-router";
import { useAuth } from "~/hooks/use-auth";

/* ------------------------------ ícones SVG ------------------------------ */

interface IconProps {
  className?: string;
}

function IconBars({ className = "h-5 w-5" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"
      />
    </svg>
  );
}

function IconArrowRight({ className = "h-5 w-5" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M13.5 4.5 21 12m0 0-7.5 7.5M21 12H3"
      />
    </svg>
  );
}

function IconPhone({ className = "h-6 w-6" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M10.5 1.5H8.25A2.25 2.25 0 0 0 6 3.75v16.5a2.25 2.25 0 0 0 2.25 2.25h7.5A2.25 2.25 0 0 0 18 20.25V3.75a2.25 2.25 0 0 0-2.25-2.25H13.5m-3 0V3h3V1.5m-3 0h3m-3 18.75h3"
      />
    </svg>
  );
}

function IconChat({ className = "h-6 w-6" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z"
      />
    </svg>
  );
}

function IconChart({ className = "h-6 w-6" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 0 1 3 19.875v-6.75ZM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V8.625ZM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 0 1-1.125-1.125V4.125Z"
      />
    </svg>
  );
}

function IconBolt({ className = "h-6 w-6" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="m3.75 13.5 10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75Z"
      />
    </svg>
  );
}

function IconTruck({ className = "h-6 w-6" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M8.25 18.75a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m3 0h6m-9 0H3.375a1.125 1.125 0 0 1-1.125-1.125V14.25m17.25 4.5a1.5 1.5 0 0 1-3 0m3 0a1.5 1.5 0 0 0-3 0m3 0h1.125c.621 0 1.129-.504 1.09-1.124a17.902 17.902 0 0 0-3.213-9.193 2.056 2.056 0 0 0-1.58-.86H14.25M16.5 18.75h-2.25m0-11.177v-.958c0-.568-.422-1.048-.987-1.106a48.554 48.554 0 0 0-10.026 0 1.106 1.106 0 0 0-.987 1.106v7.635m12-6.677v6.677m0 4.5v-4.5m0 0h-12"
      />
    </svg>
  );
}

function IconBell({ className = "h-6 w-6" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0"
      />
    </svg>
  );
}

function IconDocument({ className = "h-6 w-6" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z"
      />
    </svg>
  );
}

function IconSquares({ className = "h-6 w-6" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M3.75 6A2.25 2.25 0 0 1 6 3.75h2.25A2.25 2.25 0 0 1 10.5 6v2.25a2.25 2.25 0 0 1-2.25 2.25H6a2.25 2.25 0 0 1-2.25-2.25V6ZM3.75 15.75A2.25 2.25 0 0 1 6 13.5h2.25a2.25 2.25 0 0 1 2.25 2.25V18a2.25 2.25 0 0 1-2.25 2.25H6A2.25 2.25 0 0 1 3.75 18v-2.25ZM13.5 6a2.25 2.25 0 0 1 2.25-2.25H18A2.25 2.25 0 0 1 20.25 6v2.25A2.25 2.25 0 0 1 18 10.5h-2.25a2.25 2.25 0 0 1-2.25-2.25V6ZM13.5 15.75a2.25 2.25 0 0 1 2.25-2.25H18a2.25 2.25 0 0 1 2.25 2.25V18A2.25 2.25 0 0 1 18 20.25h-2.25A2.25 2.25 0 0 1 13.5 18v-2.25Z"
      />
    </svg>
  );
}

function IconUsers({ className = "h-6 w-6" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M15 19.128a9.38 9.38 0 0 0 2.625.372 9.337 9.337 0 0 0 4.121-.952 4.125 4.125 0 0 0-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 0 1 8.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0 1 11.964-3.07M12 6.375a3.375 3.375 0 1 1-6.75 0 3.375 3.375 0 0 1 6.75 0Zm8.25 2.25a2.625 2.625 0 1 1-5.25 0 2.625 2.625 0 0 1 5.25 0Z"
      />
    </svg>
  );
}

function IconCheckBadge({ className = "h-5 w-5" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M9 12.75 11.25 15 15 9.75M21 12c0 1.268-.63 2.39-1.593 3.068a3.745 3.745 0 0 1-1.043 3.296 3.745 3.745 0 0 1-3.296 1.043A3.745 3.745 0 0 1 12 21c-1.268 0-2.39-.63-3.068-1.593a3.746 3.746 0 0 1-3.296-1.043 3.745 3.745 0 0 1-1.043-3.296A3.745 3.745 0 0 1 3 12c0-1.268.63-2.39 1.593-3.068a3.745 3.745 0 0 1 1.043-3.296 3.746 3.746 0 0 1 3.296-1.043A3.746 3.746 0 0 1 12 3c1.268 0 2.39.63 3.068 1.593a3.746 3.746 0 0 1 3.296 1.043 3.746 3.746 0 0 1 1.043 3.296A3.745 3.745 0 0 1 21 12Z"
      />
    </svg>
  );
}

function IconCalendar({ className = "h-4 w-4" }: IconProps) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
      strokeWidth={1.5}
      stroke="currentColor"
      className={className}
      aria-hidden="true"
    >
      <path
        strokeLinecap="round"
        strokeLinejoin="round"
        d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 0 1 2.25-2.25h13.5A2.25 2.25 0 0 1 21 7.5v11.25m-18 0A2.25 2.25 0 0 0 5.25 21h13.5A2.25 2.25 0 0 0 21 18.75m-18 0v-7.5A2.25 2.25 0 0 1 5.25 9h13.5A2.25 2.25 0 0 1 21 11.25v7.5"
      />
    </svg>
  );
}

/* --------------------------------- dados --------------------------------- */

const NAV_LINKS = [
  { href: "#como-funciona", label: "Como funciona" },
  { href: "#recursos", label: "Recursos" },
  { href: "#contato", label: "Contato" },
];

const TRUST_STATS = [
  {
    icon: <IconPhone />,
    value: "100% pelo celular",
    desc: "Monte o pedido e agende a entrega de onde estiver",
  },
  {
    icon: <IconChat />,
    value: "Aviso por WhatsApp",
    desc: "Cada etapa, do pedido até a entrega",
  },
  {
    icon: <IconChart />,
    value: "Relatórios diários",
    desc: "Produção e financeiro sempre em dia",
  },
];

const FEATURES = [
  {
    icon: <IconBolt />,
    title: "Pedidos rápidos",
    description:
      "Catálogo sempre à mão: monte o pedido em segundos e agende a data de entrega.",
  },
  {
    icon: <IconTruck />,
    title: "Acompanhamento de status",
    description:
      "Saiba se o pedido está em produção, pronto ou entregue, em tempo real.",
  },
  {
    icon: <IconBell />,
    title: "Notificações WhatsApp e e-mail",
    description:
      "Avisos automáticos em cada etapa — a produção e o solicitante sempre sabem o que fazer.",
  },
  {
    icon: <IconDocument />,
    title: "Relatórios de produção e financeiro",
    description:
      "O que foi produzido, quanto entrou e o que ainda está pendente, organizado por dia.",
  },
  {
    icon: <IconSquares />,
    title: "Catálogo gerenciável",
    description:
      "Produtos, preços e disponibilidade são atualizados pela própria padaria.",
  },
  {
    icon: <IconUsers />,
    title: "Acesso por perfil",
    description:
      "Solicitante, produção e administração: cada perfil vê exatamente o que precisa.",
  },
];

const STEP_CHIP_COLORS = [
  "bg-primary/15 text-primary",
  "bg-secondary/15 text-secondary",
  "bg-accent/15 text-accent",
];

/* --------------------------------- página --------------------------------- */

export default function LandingPage() {
  const { isAuthenticated } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  const year = new Date().getFullYear();

  return (
    <div className="min-h-screen bg-base-100">
      {/* ------------------------------ Navbar ------------------------------ */}
      <header className="navbar sticky top-0 z-40 border-b border-base-200 bg-base-100/80 backdrop-blur-md">
        <div className="navbar-start">
          <Link to="/" className="flex items-center gap-2 px-2">
            <img
              src="/logo.svg"
              alt="Logo Mana Paes"
              className="h-9 w-9 rounded-box"
            />
            <span className="text-lg font-bold tracking-tight">Mana Paes</span>
          </Link>
        </div>

        <nav className="navbar-center hidden lg:flex" aria-label="Navegação principal">
          <ul className="menu menu-horizontal gap-1 px-1">
            {NAV_LINKS.map((link) => (
              <li key={link.href}>
                <a href={link.href} className="link link-hover">
                  {link.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        <div className="navbar-end gap-2">
          <Link to="/login" className="btn btn-ghost btn-sm hidden sm:inline-flex">
            Entrar
          </Link>
          <Link to="/register" className="btn btn-primary btn-sm">
            Criar conta
          </Link>

          <details
            className="dropdown dropdown-end lg:hidden"
            open={menuOpen}
            onToggle={(event) => setMenuOpen(event.currentTarget.open)}
          >
            <summary
              className="btn btn-ghost btn-circle"
              aria-label="Abrir menu"
            >
              <IconBars />
            </summary>
            <ul className="dropdown-content menu z-50 w-56 rounded-box border border-base-200 bg-base-100 p-2 shadow-lg">
              {NAV_LINKS.map((link) => (
                <li key={link.href}>
                  <a href={link.href} onClick={() => setMenuOpen(false)}>
                    {link.label}
                  </a>
                </li>
              ))}
              <li className="divider my-1" />
              <li>
                <Link to="/login" onClick={() => setMenuOpen(false)}>
                  Entrar
                </Link>
              </li>
              <li className="mt-1">
                <Link
                  to="/register"
                  onClick={() => setMenuOpen(false)}
                  className="btn btn-primary btn-sm btn-block"
                >
                  Criar conta
                </Link>
              </li>
            </ul>
          </details>
        </div>
      </header>

      {/* ------------------------------- Hero ------------------------------- */}
      <section className="relative overflow-hidden bg-gradient-to-b from-primary/15 via-base-100 to-base-100">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -top-32 right-[-10%] h-96 w-96 rounded-full bg-primary/25 blur-3xl"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute bottom-[-25%] left-[-10%] h-96 w-96 rounded-full bg-secondary/15 blur-3xl"
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute left-1/2 top-1/3 h-64 w-64 rounded-full bg-accent/10 blur-3xl"
        />

        <div className="hero">
          <div className="hero-content mx-auto flex w-full max-w-7xl flex-col items-center gap-12 px-4 py-16 text-center md:py-24 lg:flex-row lg:gap-16 lg:text-left">
            {/* Texto */}
            <div className="flex-1">
              <span className="badge badge-primary badge-lg">
                Sistema de pedidos para padarias
              </span>
              <h1 className="mt-5 text-balance text-4xl font-black leading-tight tracking-tight md:text-5xl lg:text-6xl">
                Encomende seus pães com{" "}
                <span className="text-primary">poucos toques</span>
              </h1>
              <p className="mx-auto mt-5 max-w-xl text-lg text-base-content/70 lg:mx-0">
                O Mana Paes organiza os pedidos da padaria do começo ao fim:
                você escolhe os produtos, agenda a entrega e acompanha tudo pelo
                celular — sem troca de mensagens.
              </p>
              <div className="mt-8 flex flex-col items-center gap-3 sm:flex-row sm:justify-center lg:justify-start">
                <Link
                  to="/register"
                  className="btn btn-primary btn-lg w-full sm:w-auto"
                >
                  Fazer meu primeiro pedido
                  <IconArrowRight />
                </Link>
                <Link
                  to="/login"
                  className="btn btn-outline btn-lg w-full sm:w-auto"
                >
                  Já tenho conta
                </Link>
              </div>
              <p className="mt-5 text-sm text-base-content/50">
                Criação de conta grátis · Funciona no celular · Sem instalação
              </p>
            </div>

            {/* Mockup visual */}
            <div className="relative mx-auto w-full max-w-sm flex-1">
              <div
                aria-hidden="true"
                className="absolute -right-5 -top-5 h-24 w-24 rounded-full border-4 border-primary/20"
              />
              <div
                aria-hidden="true"
                className="absolute -bottom-8 -right-10 h-32 w-32 rounded-full border-4 border-secondary/15"
              />

              <div className="card card-border border-base-200 bg-base-100 shadow-2xl transition-transform duration-300 hover:-translate-y-1">
                <div className="card-body gap-5">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-xs font-medium uppercase tracking-wide text-base-content/50">
                        Pedido #124
                      </p>
                      <p className="text-lg font-bold tracking-tight">
                        Encomenda do dia
                      </p>
                    </div>
                    <span className="badge badge-warning badge-sm">
                      Em produção
                    </span>
                  </div>

                  <div className="space-y-2 text-sm">
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-base-content/70">
                        Pão francês{" "}
                        <span className="text-base-content/40">× 40</span>
                      </span>
                      <span className="font-semibold">R$ 36,00</span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-base-content/70">
                        Sonho com creme{" "}
                        <span className="text-base-content/40">× 12</span>
                      </span>
                      <span className="font-semibold">R$ 30,00</span>
                    </div>
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-base-content/70">
                        Bolo de fubá{" "}
                        <span className="text-base-content/40">× 1</span>
                      </span>
                      <span className="font-semibold">R$ 24,00</span>
                    </div>
                  </div>

                  <div className="divider divider-primary my-0 text-xs font-medium">
                    Entrega agendada
                  </div>

                  <div className="flex items-center gap-2 text-sm">
                    <IconCalendar className="h-4 w-4 text-primary" />
                    <span className="font-medium">
                      Sexta-feira, 28/08 — às 7h
                    </span>
                  </div>

                  <div className="flex items-center justify-between gap-3">
                    <span className="text-xs text-base-content/50">
                      Status do pedido
                    </span>
                    <span className="badge badge-soft badge-success badge-sm">
                      Saindo do forno
                    </span>
                  </div>
                  <progress
                    className="progress progress-primary h-2"
                    value={70}
                    max={100}
                  />
                </div>
              </div>

              <div className="card absolute -bottom-5 -left-4 rotate-[-2deg] bg-success text-success-content shadow-xl transition-transform duration-300 hover:rotate-0 sm:-left-8">
                <div className="card-body flex-row items-center gap-3 p-3">
                  <IconCheckBadge />
                  <div className="text-left text-xs">
                    <p className="font-bold">Pedido confirmado</p>
                    <p className="opacity-80">Aviso enviado no WhatsApp</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* --------------------------- Barra de confiança --------------------------- */}
      <section className="relative z-10 mx-auto -mt-8 max-w-7xl px-4">
        <div className="stats stats-vertical w-full border border-base-200 bg-base-100 shadow-lg sm:stats-horizontal">
          {TRUST_STATS.map((stat) => (
            <div className="stat" key={stat.value}>
              <div className="stat-figure text-primary">{stat.icon}</div>
              <div className="stat-value text-2xl">{stat.value}</div>
              <div className="stat-desc">{stat.desc}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ----------------------------- Como funciona ----------------------------- */}
      <section
        id="como-funciona"
        className="scroll-mt-24 bg-base-200/60 py-16 md:py-24"
      >
        <div className="mx-auto max-w-7xl px-4">
          <div className="mx-auto max-w-2xl text-center">
            <span className="badge badge-soft badge-primary">Como funciona</span>
            <h2 className="mt-4 text-balance text-3xl font-black tracking-tight md:text-4xl">
              Do catálogo ao forno, sem atropelo
            </h2>
            <p className="mt-3 text-base-content/70">
              Quatro passos simples que resumem o dia a dia de um pedido.
            </p>
          </div>

          <ol className="steps steps-vertical mt-12 lg:steps-horizontal">
            <li className="step step-primary" data-content="1">
              <div className="text-left">
                <p className="font-semibold">Escolha e agende</p>
                <p className="text-sm text-base-content/60">
                  Selecione os produtos e a data de entrega no catálogo.
                </p>
              </div>
            </li>
            <li className="step step-primary" data-content="2">
              <div className="text-left">
                <p className="font-semibold">Produção recebe na hora</p>
                <p className="text-sm text-base-content/60">
                  Sem telefonemas: o pedido chega direto para quem vai produzir.
                </p>
              </div>
            </li>
            <li className="step step-primary" data-content="3">
              <div className="text-left">
                <p className="font-semibold">Acompanhe o status</p>
                <p className="text-sm text-base-content/60">
                  Em produção, pronto ou entregue — sempre atualizado.
                </p>
              </div>
            </li>
            <li className="step step-primary" data-content="4">
              <div className="text-left">
                <p className="font-semibold">Confirmação por WhatsApp</p>
                <p className="text-sm text-base-content/60">
                  Você recebe o aviso quando o pedido sai do forno.
                </p>
              </div>
            </li>
          </ol>
        </div>
      </section>

      {/* ------------------------------- Recursos ------------------------------- */}
      <section id="recursos" className="scroll-mt-24 py-16 md:py-24">
        <div className="mx-auto max-w-7xl px-4">
          <div className="mx-auto max-w-2xl text-center">
            <span className="badge badge-soft badge-secondary">Recursos</span>
            <h2 className="mt-4 text-balance text-3xl font-black tracking-tight md:text-4xl">
              Tudo o que a padaria precisa, em um só lugar
            </h2>
            <p className="mt-3 text-base-content/70">
              Ferramentas diretas para quem pede, para quem produz e para quem
              administra.
            </p>
          </div>

          <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {FEATURES.map((feature, index) => (
              <div
                key={feature.title}
                className="card card-border bg-base-100 transition-transform duration-300 hover:-translate-y-1"
              >
                <div className="card-body">
                  <div
                    className={`flex h-11 w-11 items-center justify-center rounded-box ${
                      STEP_CHIP_COLORS[index % STEP_CHIP_COLORS.length]
                    }`}
                  >
                    {feature.icon}
                  </div>
                  <h3 className="card-title mt-3 text-lg">{feature.title}</h3>
                  <p className="text-sm text-base-content/70">
                    {feature.description}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ------------------------------ CTA final ------------------------------ */}
      <section className="mx-auto max-w-7xl px-4 pb-16 md:pb-24">
        <div className="card bg-gradient-to-br from-primary via-primary to-accent shadow-xl">
          <div className="card-body items-center py-12 text-center md:py-16">
            <h2 className="text-balance text-3xl font-black tracking-tight text-primary-content md:text-4xl">
              Pronto para começar?
            </h2>
            <p className="mt-2 max-w-xl text-primary-content/80">
              Crie sua conta em menos de um minuto e faça seu primeiro pedido
              ainda hoje.
            </p>
            <div className="card-actions mt-6 flex-col gap-3 sm:flex-row">
              <Link
                to="/register"
                className="btn btn-lg bg-base-100 text-base-content"
              >
                Criar conta grátis
              </Link>
              <a
                href="#contato"
                className="btn btn-lg border-primary-content/40 bg-transparent text-primary-content hover:border-primary-content hover:bg-primary-content/10"
              >
                Fale conosco
              </a>
            </div>
          </div>
        </div>
      </section>

      {/* -------------------------------- Footer -------------------------------- */}
      <footer
        id="contato"
        className="scroll-mt-24 border-t border-base-200 bg-base-200"
      >
        <div className="footer footer-vertical mx-auto max-w-7xl px-4 py-12 sm:footer-horizontal">
          <aside className="max-w-sm">
            <img
              src="/logo.svg"
              alt="Logo Mana Paes"
              className="h-10 w-10 rounded-box"
            />
            <p className="text-sm text-base-content/70">
              Sistema de pedidos para padarias — do catálogo à entrega, tudo em
              um só lugar.
            </p>
          </aside>

          <nav aria-label="Acesso">
            <h6 className="footer-title">Acesso</h6>
            <Link to="/login" className="link link-hover">
              Entrar
            </Link>
            <Link to="/register" className="link link-hover">
              Criar conta
            </Link>
          </nav>

          <nav aria-label="Navegação">
            <h6 className="footer-title">Página</h6>
            <a href="#como-funciona" className="link link-hover">
              Como funciona
            </a>
            <a href="#recursos" className="link link-hover">
              Recursos
            </a>
          </nav>

          <nav aria-label="Contato">
            <h6 className="footer-title">Contato</h6>
            <a
              href="mailto:contato@manapaes.com.br"
              className="link link-hover"
            >
              contato@manapaes.com.br
            </a>
            <a href="#contato" className="link link-hover">
              WhatsApp
            </a>
          </nav>
        </div>

        <div className="border-t border-base-300/60">
          <div className="mx-auto flex max-w-7xl flex-col gap-1 px-4 py-4 text-sm text-base-content/60 sm:flex-row sm:items-center sm:justify-between">
            <p>© {year} Mana Paes. Todos os direitos reservados.</p>
            <p>Feito com carinho para padarias.</p>
          </div>
        </div>
      </footer>
    </div>
  );
}
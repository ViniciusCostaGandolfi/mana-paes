import { useEffect, useState } from "react";
import { Link, Navigate, NavLink, Outlet } from "react-router";
import { useAuth } from "~/hooks/use-auth";
import { cn } from "~/lib/utils";
import { ThemeToggle } from "~/components/ui/theme-toggle";
import { UserMenu } from "./user-menu";

interface NavItem {
  to: string;
  label: string;
  end?: boolean;
}

export function AppLayout() {
  const { isAuthenticated, isAdmin, isRequester, isProduction } = useAuth();
  const [isOffline, setIsOffline] = useState<boolean>(
    () => typeof navigator !== "undefined" && !navigator.onLine,
  );

  useEffect(() => {
    const handleOffline = () => setIsOffline(true);
    const handleOnline = () => setIsOffline(false);
    window.addEventListener("offline", handleOffline);
    window.addEventListener("online", handleOnline);
    return () => {
      window.removeEventListener("offline", handleOffline);
      window.removeEventListener("online", handleOnline);
    };
  }, []);

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const navItems: NavItem[] = [{ to: "/dashboard", label: "Dashboard", end: true }];

  if (isRequester) {
    navItems.push(
      { to: "/orders", label: "Meus Pedidos" },
      { to: "/orders/new", label: "Novo Pedido" },
    );
  }

  if (isProduction) {
    navItems.push(
      { to: "/orders", label: "Pedidos" },
      { to: "/reports/production", label: "Relatório de Produção" },
    );
  }

  if (isAdmin) {
    navItems.push(
      { to: "/orders", label: "Pedidos" },
      { to: "/products", label: "Produtos" },
      { to: "/users", label: "Solicitantes" },
      { to: "/reports/production", label: "Relatório de Produção" },
      { to: "/reports/financial", label: "Relatório Financeiro" },
      { to: "/settings/notifications", label: "Configurações" },
      { to: "/settings/whatsapp", label: "WhatsApp" },
    );
  }

  return (
    <div className="drawer lg:drawer-open">
      <input id="app-drawer" type="checkbox" className="drawer-toggle" />

      <div className="drawer-content flex min-h-screen flex-col bg-base-100">
        {isOffline ? (
          <div className="bg-warning text-warning-content px-4 py-1 text-center text-sm">
            Offline — você está sem conexão com a internet.
          </div>
        ) : null}

        <header className="navbar sticky top-0 z-30 border-b border-base-200 bg-base-100">
          <div className="navbar-start">
            <label
              htmlFor="app-drawer"
              className="btn btn-ghost btn-circle drawer-button lg:hidden"
              aria-label="Abrir menu"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={1.5}
                stroke="currentColor"
                className="h-6 w-6"
                aria-hidden="true"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"
                />
              </svg>
            </label>
            <Link
              to="/dashboard"
              className="px-2 text-lg font-bold tracking-tight text-base-content"
            >
              Mana Paes
            </Link>
          </div>

          <div className="navbar-center hidden sm:flex" />

          <div className="navbar-end gap-1">
            <ThemeToggle />
            <UserMenu />
          </div>
        </header>

        <main className="mx-auto w-full max-w-7xl flex-1 p-4 md:p-6">
          <Outlet />
        </main>
      </div>

      <div className="drawer-side">
        <label
          htmlFor="app-drawer"
          aria-label="Fechar menu"
          className="drawer-overlay"
        />
        <aside className="min-h-full w-72 border-r border-base-200 bg-base-100">
          <div className="flex items-center px-5 py-4">
            <Link
              to="/dashboard"
              className="text-lg font-bold tracking-tight text-base-content"
            >
              Mana Paes
            </Link>
          </div>
          <ul className="menu w-full p-2 text-base-content">
            {navItems.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    cn(isActive && "menu-active")
                  }
                >
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </aside>
      </div>
    </div>
  );
}

export default AppLayout;
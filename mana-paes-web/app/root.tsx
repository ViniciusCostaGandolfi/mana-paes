import { useEffect } from "react";
import { registerSW } from "virtual:pwa-register";
import {
  isRouteErrorResponse,
  Links,
  Meta,
  Outlet,
  Scripts,
  ScrollRestoration,
} from "react-router";
import { QueryClientProvider } from "@tanstack/react-query";
import type { ReactNode } from "react";

import type { Route } from "./+types/root";
import { queryClient } from "~/lib/query";
import { AuthProvider } from "~/hooks/use-auth";
import "./app.css";

export function Layout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR" className="scroll-smooth">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta
          name="description"
          content="Mana Paes — sistema de gerenciamento de pedidos da padaria."
        />
        <meta name="theme-color" content="#b45309" />
        <title>Mana Paes</title>
        {import.meta.env.PROD ? (
          <link rel="manifest" href="/manifest.webmanifest" />
        ) : null}
        <Meta />
        <Links />
      </head>
      <body className="bg-base-100 text-base-content">
        {children}
        <ScrollRestoration />
        <Scripts />
      </body>
    </html>
  );
}

export default function App() {
  // Registra o service worker do PWA (no-op em desenvolvimento; ativo apenas
  // em produção, onde o vite-plugin-pwa gera sw.js + manifest.webmanifest).
  useEffect(() => {
    if (import.meta.env.PROD) {
      registerSW({ immediate: true });
    }
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <Outlet />
      </AuthProvider>
    </QueryClientProvider>
  );
}

export function ErrorBoundary({ error }: Route.ErrorBoundaryProps) {
  let title = "Algo deu errado";
  let details = "Ocorreu um erro inesperado. Tente recarregar a página.";

  if (isRouteErrorResponse(error)) {
    if (error.status === 404) {
      title = "Página não encontrada";
      details = "A página que você procura não existe ou foi movida.";
    } else {
      title = `Erro ${error.status}`;
      details = error.statusText || details;
    }
  } else if (error instanceof Error && error.message) {
    details = error.message;
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-base-100 p-4">
      <div className="card w-full max-w-md border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <h1 className="card-title text-2xl">{title}</h1>
          <p className="text-base-content/70">{details}</p>
          <button
            type="button"
            className="btn btn-primary mt-4"
            onClick={() => window.location.reload()}
          >
            Recarregar página
          </button>
        </div>
      </div>
    </main>
  );
}
import { useEffect, useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";

import { AsyncState } from "~/components/ui/async-state";
import { ConfirmDialog } from "~/components/ui/confirm-dialog";
import { ErrorAlert } from "~/components/ui/error-alert";
import { PageHeader } from "~/components/ui/page-header";
import { getApiErrorMessage } from "~/lib/api";
import {
  connectWhatsapp,
  disconnectWhatsapp,
  getWhatsappStatus,
  simulateScan,
  testWhatsapp,
} from "~/services/whatsapp.service";
import type {
  WhatsAppConnectionState,
  WhatsAppStatusResponse,
} from "~/types/api";

type ConnectionPhase = "loading" | "disconnected" | "connecting" | "connected";

const POLLING_INTERVAL_MS = 3_000;

/**
 * Formata o número conectado em um formato amigável para exibição.
 * Ex.: "5511999999999" → "+55 11 99999-9999".
 */
function formatWhatsappNumber(value: string | null | undefined): string {
  const digits = (value ?? "").replace(/\D/g, "");
  if (!digits) return "";

  if (digits.length === 13 && digits.startsWith("55")) {
    return `+${digits.slice(0, 2)} ${digits.slice(2, 4)} ${digits.slice(4, 9)}-${digits.slice(9)}`;
  }
  if (digits.length === 12 && digits.startsWith("55")) {
    return `+${digits.slice(0, 2)} ${digits.slice(2, 4)} ${digits.slice(4, 8)}-${digits.slice(8)}`;
  }
  return `+${digits}`;
}

export default function WhatsappSettingsPage() {
  const [status, setStatus] = useState<WhatsAppStatusResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [confirmDisconnectOpen, setConfirmDisconnectOpen] = useState(false);
  const [loadAttempt, setLoadAttempt] = useState(0);
  const prevStateRef = useRef<WhatsAppConnectionState | null>(null);

  const phase: ConnectionPhase = isLoading
    ? "loading"
    : status?.state === "OPEN"
      ? "connected"
      : status?.state === "CONNECTING"
        ? "connecting"
        : "disconnected";

  // Carga inicial do status.
  useEffect(() => {
    let active = true;
    setIsLoading(true);
    setLoadError(null);

    getWhatsappStatus()
      .then((data) => {
        if (!active) return;
        setStatus(data);
        prevStateRef.current = data.state;
      })
      .catch((error) => {
        if (!active) return;
        setLoadError(getApiErrorMessage(error));
      })
      .finally(() => {
        if (active) setIsLoading(false);
      });

    return () => {
      active = false;
    };
  }, [loadAttempt]);

  // Polling do status enquanto a conexão está em andamento. Para quando o
  // estado transiciona para OPEN (o effect é recriado a cada mudança de fase).
  useEffect(() => {
    if (phase !== "connecting") return;

    const interval = window.setInterval(() => {
      getWhatsappStatus()
        .then((data) => {
          const prevState = prevStateRef.current;
          setStatus(data);
          prevStateRef.current = data.state;
          setActionError(null);

          if (prevState === "CONNECTING" && data.state === "OPEN") {
            setSuccessMessage("WhatsApp conectado!");
          }
        })
        .catch((error) => {
          setActionError(getApiErrorMessage(error));
        });
    }, POLLING_INTERVAL_MS);

    return () => window.clearInterval(interval);
  }, [phase]);

  const resetToDisconnected = () => {
    setStatus({ state: "CLOSE", qrCodeBase64: null, connectedNumber: null });
    prevStateRef.current = "CLOSE";
    setSuccessMessage(null);
  };

  const connectMutation = useMutation({
    mutationFn: connectWhatsapp,
    onSuccess: (data) => {
      setStatus(data);
      prevStateRef.current = data.state;
      setActionError(null);
      setSuccessMessage(null);
    },
    onError: (error) => setActionError(getApiErrorMessage(error)),
  });

  const disconnectMutation = useMutation({
    mutationFn: disconnectWhatsapp,
    onSuccess: () => {
      resetToDisconnected();
      setActionError(null);
      setConfirmDisconnectOpen(false);
    },
    onError: (error) => {
      setActionError(getApiErrorMessage(error));
      setConfirmDisconnectOpen(false);
    },
  });

  const testMutation = useMutation({
    mutationFn: testWhatsapp,
    onSuccess: (data) => {
      setActionError(null);
      setSuccessMessage(data.message);
    },
    onError: (error) => setActionError(getApiErrorMessage(error)),
  });

  const simulateMutation = useMutation({
    mutationFn: simulateScan,
    onSuccess: (data) => {
      const prevState = prevStateRef.current;
      setStatus(data);
      prevStateRef.current = data.state;
      setActionError(null);

      if (prevState === "CONNECTING" && data.state === "OPEN") {
        setSuccessMessage("WhatsApp conectado!");
      }
    },
  });

  // Durante a conexão, "Cancelar" sempre volta a UI para o estado desconectado,
  // mesmo que o backend falhe ao fechar a sessão.
  const handleCancelConnection = () => {
    disconnectMutation.mutate(undefined, {
      onSuccess: resetToDisconnected,
      onError: () => resetToDisconnected(),
    });
  };

  const connectedNumber = formatWhatsappNumber(status?.connectedNumber);

  return (
    <div className="space-y-6">
      <PageHeader
        title="WhatsApp"
        subtitle="Conecte a conta do WhatsApp usada para enviar notificações e relatórios automáticos."
      />

      <div className="card bg-base-100 shadow">
        <div className="card-body">
          <h2 className="card-title">Conexão com o WhatsApp</h2>
          <p className="text-sm text-base-content/70">
            A conexão é feita por QR code, escaneado no aplicativo do WhatsApp.
          </p>

          <div className="space-y-4">
            {successMessage ? (
              <div role="alert" className="alert alert-success sm:alert-horizontal">
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  className="h-6 w-6 shrink-0"
                  aria-hidden="true"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                  />
                </svg>
                <span className="flex-1">{successMessage}</span>
              </div>
            ) : null}

            <AsyncState
              isLoading={isLoading}
              isError={!!loadError}
              error={loadError}
              onRetry={() => setLoadAttempt((n) => n + 1)}
            >
              {phase === "disconnected" ? (
                <div className="space-y-4">
                  <span className="badge badge-neutral">Desconectado</span>
                  <p className="text-sm text-base-content/70">
                    Nenhuma conta do WhatsApp está conectada. Conecte para
                    permitir o envio de notificações e relatórios.
                  </p>
                  <button
                    type="button"
                    className="btn btn-primary"
                    disabled={connectMutation.isPending}
                    onClick={() => connectMutation.mutate()}
                  >
                    {connectMutation.isPending ? (
                      <span
                        className="loading loading-spinner loading-sm"
                        aria-hidden="true"
                      />
                    ) : null}
                    Conectar WhatsApp
                  </button>
                </div>
              ) : phase === "connecting" ? (
                <div className="space-y-4">
                  <span className="badge badge-warning badge-lg gap-2">
                    <span
                      className="loading loading-spinner loading-xs"
                      aria-hidden="true"
                    />
                    Aguardando leitura...
                  </span>

                  {status?.qrCodeBase64 ? (
                    <img
                      src={status.qrCodeBase64}
                      alt="QR code para conectar o WhatsApp"
                      className="mx-auto w-64 rounded-box border border-base-300 bg-white p-3"
                    />
                  ) : (
                    <div className="flex items-center justify-center py-16">
                      <span
                        className="loading loading-spinner loading-lg"
                        aria-hidden="true"
                      />
                    </div>
                  )}

                  <p className="text-center text-sm text-base-content/70">
                    Escaneie o QR code com o WhatsApp{" "}
                    <span className="font-medium text-base-content">
                      (WhatsApp &gt; Aparelhos conectados &gt; Conectar um
                      aparelho)
                    </span>
                  </p>

                  <div>
                    <button
                      type="button"
                      className="btn btn-outline"
                      disabled={disconnectMutation.isPending}
                      onClick={handleCancelConnection}
                    >
                      {disconnectMutation.isPending ? (
                        <span
                          className="loading loading-spinner loading-sm"
                          aria-hidden="true"
                        />
                      ) : null}
                      Cancelar
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-4">
                  <div role="alert" className="alert alert-success sm:alert-horizontal">
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                      strokeWidth={1.5}
                      stroke="currentColor"
                      className="h-6 w-6 shrink-0"
                      aria-hidden="true"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
                      />
                    </svg>
                    <span className="flex-1">
                      <span className="font-medium">Conectado</span>
                      {connectedNumber ? (
                        <>
                          {" · "}
                          {connectedNumber}
                        </>
                      ) : null}
                    </span>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <button
                      type="button"
                      className="btn btn-outline"
                      disabled={testMutation.isPending}
                      onClick={() => testMutation.mutate()}
                    >
                      {testMutation.isPending ? (
                        <span
                          className="loading loading-spinner loading-sm"
                          aria-hidden="true"
                        />
                      ) : null}
                      Testar conexão
                    </button>
                    <button
                      type="button"
                      className="btn btn-outline btn-error"
                      disabled={disconnectMutation.isPending}
                      onClick={() => setConfirmDisconnectOpen(true)}
                    >
                      Desconectar
                    </button>
                  </div>
                </div>
              )}
            </AsyncState>

            {actionError ? <ErrorAlert error={actionError} /> : null}
          </div>
        </div>
      </div>

      {import.meta.env.DEV ? (
        <div className="card bg-base-100 shadow">
          <div className="card-body">
            <h2 className="card-title">Ambiente de desenvolvimento</h2>
            <p className="text-sm text-base-content/70">
              Simula a leitura do QR code, permitindo testar o fluxo sem um
              aparelho real conectado à Evolution API.
            </p>
            <div className="space-y-4">
              <button
                type="button"
                className="btn btn-outline"
                disabled={simulateMutation.isPending}
                onClick={() => simulateMutation.mutate()}
              >
                {simulateMutation.isPending ? (
                  <span
                    className="loading loading-spinner loading-sm"
                    aria-hidden="true"
                  />
                ) : null}
                Simular escaneamento (dev)
              </button>

              {simulateMutation.isError ? (
                <ErrorAlert error={getApiErrorMessage(simulateMutation.error)} />
              ) : null}
            </div>
          </div>
        </div>
      ) : null}

      <ConfirmDialog
        open={confirmDisconnectOpen}
        title="Desconectar WhatsApp"
        message="Tem certeza que deseja desconectar o WhatsApp? As notificações e relatórios automáticos ficarão suspensos até uma nova conexão."
        confirmLabel="Desconectar"
        cancelLabel="Cancelar"
        danger
        isLoading={disconnectMutation.isPending}
        onConfirm={() => disconnectMutation.mutate()}
        onCancel={() => setConfirmDisconnectOpen(false)}
      />
    </div>
  );
}
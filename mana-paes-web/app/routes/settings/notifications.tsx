import { useEffect, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { AsyncState } from "~/components/ui/async-state";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";
import { PageHeader } from "~/components/ui/page-header";
import { Pagination } from "~/components/ui/pagination";
import { getApiErrorMessage } from "~/lib/api";
import { queryClient } from "~/lib/query";
import {
  cn,
  formatDateTime,
  NOTIFICATION_CHANNEL_LABEL,
  NOTIFICATION_STATUS_LABEL,
  NOTIFICATION_TYPE_LABEL,
} from "~/lib/utils";
import {
  getConfig,
  listLogs,
  testWhatsapp,
  updateConfig,
} from "~/services/notification.service";
import type {
  NotificationChannel,
  NotificationConfigRequest,
  NotificationStatus,
} from "~/types/api";

const configSchema = z.object({
  adminWhatsappNumber: z.string(),
  adminEmail: z.string().email().or(z.literal("")),
  dailyReportTime: z.string(),
  whatsappEnabled: z.boolean(),
  emailEnabled: z.boolean(),
  evolutionApiInstanceName: z.string(),
  evolutionApiKey: z.string(),
});

type ConfigFormValues = z.infer<typeof configSchema>;

const LOG_STATUS_BADGE_CLASS: Record<NotificationStatus, string> = {
  SENT: "badge-success",
  PENDING: "badge-warning",
  FAILED: "badge-error",
};

const LOGS_PAGE_SIZE = 15;

/**
 * Envia apenas os campos preenchidos/alterados. Campos opcionais vazios são
 * omitidos do payload para não sobrescreverem valores salvos no backend.
 * O horário é convertido de "HH:mm" (input) para "HH:mm:ss" (contrato).
 */
function buildConfigPayload(values: ConfigFormValues): NotificationConfigRequest {
  const payload: NotificationConfigRequest = {};

  const whatsappNumber = values.adminWhatsappNumber.trim();
  if (whatsappNumber) payload.adminWhatsappNumber = whatsappNumber;

  const email = values.adminEmail.trim();
  if (email) payload.adminEmail = email;

  const reportTime = values.dailyReportTime.trim();
  if (reportTime) payload.dailyReportTime = `${reportTime}:00`;

  payload.whatsappEnabled = values.whatsappEnabled;
  payload.emailEnabled = values.emailEnabled;

  const instanceName = values.evolutionApiInstanceName.trim();
  if (instanceName) payload.evolutionApiInstanceName = instanceName;

  const apiKey = values.evolutionApiKey.trim();
  if (apiKey) payload.evolutionApiKey = apiKey;

  return payload;
}

export default function NotificationsSettingsPage() {
  const [status, setStatus] = useState<NotificationStatus | "">("");
  const [channel, setChannel] = useState<NotificationChannel | "">("");
  const [page, setPage] = useState(1);

  const configQuery = useQuery({
    queryKey: ["notifications-config"],
    queryFn: getConfig,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ConfigFormValues>({
    resolver: zodResolver(configSchema),
    defaultValues: {
      adminWhatsappNumber: "",
      adminEmail: "",
      dailyReportTime: "",
      whatsappEnabled: false,
      emailEnabled: false,
      evolutionApiInstanceName: "",
      evolutionApiKey: "",
    },
  });

  useEffect(() => {
    const config = configQuery.data;
    if (!config) return;
    reset({
      adminWhatsappNumber: config.adminWhatsappNumber ?? "",
      adminEmail: config.adminEmail ?? "",
      dailyReportTime: config.dailyReportTime ? config.dailyReportTime.slice(0, 5) : "",
      whatsappEnabled: config.whatsappEnabled,
      emailEnabled: config.emailEnabled,
      evolutionApiInstanceName: config.evolutionApiInstanceName ?? "",
      evolutionApiKey: "",
    });
  }, [configQuery.data, reset]);

  const updateMutation = useMutation({
    mutationFn: (values: ConfigFormValues) => updateConfig(buildConfigPayload(values)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications-config"] });
    },
  });

  const testMutation = useMutation({
    mutationFn: testWhatsapp,
  });

  const logsQuery = useQuery({
    queryKey: [
      "notification-logs",
      { status: status || undefined, channel: channel || undefined, page },
    ],
    queryFn: () =>
      listLogs({
        status: status || undefined,
        channel: channel || undefined,
        page: page - 1,
        size: LOGS_PAGE_SIZE,
      }),
  });

  const onSubmit = (values: ConfigFormValues) => {
    updateMutation.mutate(values);
  };

  const handleStatusChange = (value: NotificationStatus | "") => {
    setStatus(value);
    setPage(1);
  };

  const handleChannelChange = (value: NotificationChannel | "") => {
    setChannel(value);
    setPage(1);
  };

  return (
    <div className="space-y-6">
      <PageHeader title="Configurações de notificações" />

      <div className="card bg-base-100 shadow">
        <div className="card-body">
          <h2 className="card-title">Configurações gerais</h2>

          <AsyncState
            isLoading={configQuery.isLoading}
            isError={configQuery.isError}
            error={getApiErrorMessage(configQuery.error)}
            onRetry={() => configQuery.refetch()}
          >
            <div className="space-y-4">
              <form
                onSubmit={handleSubmit(onSubmit)}
                className="grid gap-4 sm:grid-cols-2"
              >
                <FormField
                  label="WhatsApp do administrador"
                  error={errors.adminWhatsappNumber?.message}
                  hint="Com DDI e DDD, ex.: 5511999998888"
                >
                  <input
                    type="text"
                    className="input w-full"
                    placeholder="5511999998888"
                    {...register("adminWhatsappNumber")}
                  />
                </FormField>

                <FormField
                  label="E-mail do administrador"
                  error={errors.adminEmail?.message}
                >
                  <input
                    type="email"
                    className="input w-full"
                    placeholder="admin@email.com"
                    {...register("adminEmail")}
                  />
                </FormField>

                <FormField
                  label="Horário do relatório diário"
                  error={errors.dailyReportTime?.message}
                  hint="Horário em que o relatório diário é enviado automaticamente"
                >
                  <input
                    type="time"
                    className="input w-full"
                    {...register("dailyReportTime")}
                  />
                </FormField>

                <FormField
                  label="Instância da Evolution API"
                  error={errors.evolutionApiInstanceName?.message}
                >
                  <input
                    type="text"
                    className="input w-full"
                    placeholder="Nome da instância"
                    {...register("evolutionApiInstanceName")}
                  />
                </FormField>

                <FormField
                  label="Chave da Evolution API"
                  error={errors.evolutionApiKey?.message}
                  hint="Deixe em branco para manter a chave atual"
                >
                  <input
                    type="password"
                    className="input w-full"
                    placeholder="Deixe em branco para manter a atual"
                    autoComplete="new-password"
                    {...register("evolutionApiKey")}
                  />
                </FormField>

                <label className="flex cursor-pointer items-center justify-between gap-3 rounded-box border border-base-300 px-4 py-3">
                  <span className="text-sm font-medium text-base-content">
                    Notificações por WhatsApp
                  </span>
                  <input
                    type="checkbox"
                    className="toggle toggle-primary"
                    {...register("whatsappEnabled")}
                  />
                </label>

                <label className="flex cursor-pointer items-center justify-between gap-3 rounded-box border border-base-300 px-4 py-3">
                  <span className="text-sm font-medium text-base-content">
                    Notificações por e-mail
                  </span>
                  <input
                    type="checkbox"
                    className="toggle toggle-primary"
                    {...register("emailEnabled")}
                  />
                </label>

                <div className="sm:col-span-2">
                  <button
                    type="submit"
                    className="btn btn-primary"
                    disabled={updateMutation.isPending}
                  >
                    {updateMutation.isPending ? (
                      <span
                        className="loading loading-spinner loading-sm"
                        aria-hidden="true"
                      />
                    ) : null}
                    Salvar configurações
                  </button>
                </div>
              </form>

              {updateMutation.isSuccess ? (
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
                  <span className="flex-1">Configurações salvas.</span>
                </div>
              ) : null}

              {updateMutation.isError ? (
                <ErrorAlert error={getApiErrorMessage(updateMutation.error)} />
              ) : null}
            </div>
          </AsyncState>
        </div>
      </div>

      <div className="card bg-base-100 shadow">
        <div className="card-body">
          <h2 className="card-title">WhatsApp</h2>
          <p className="text-sm text-base-content/70">
            Envia uma mensagem de teste para o número do administrador.
          </p>
          <div className="space-y-4">
            <div>
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
                Enviar mensagem de teste
              </button>
            </div>

            {testMutation.isSuccess && testMutation.data ? (
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
                <span className="flex-1">{testMutation.data.message}</span>
              </div>
            ) : null}

            {testMutation.isError ? (
              <ErrorAlert error={getApiErrorMessage(testMutation.error)} />
            ) : null}
          </div>
        </div>
      </div>

      <div className="card bg-base-100 shadow">
        <div className="card-body">
          <h2 className="card-title">Logs de notificações</h2>

          <div className="flex flex-wrap items-center gap-3">
            <label className="flex items-center gap-2">
              <span className="text-sm text-base-content/70">Status</span>
              <select
                className="select select-sm"
                value={status}
                onChange={(e) =>
                  handleStatusChange(e.target.value as NotificationStatus | "")
                }
              >
                <option value="">Todos</option>
                {(Object.keys(NOTIFICATION_STATUS_LABEL) as NotificationStatus[]).map(
                  (s) => (
                    <option key={s} value={s}>
                      {NOTIFICATION_STATUS_LABEL[s]}
                    </option>
                  ),
                )}
              </select>
            </label>

            <label className="flex items-center gap-2">
              <span className="text-sm text-base-content/70">Canal</span>
              <select
                className="select select-sm"
                value={channel}
                onChange={(e) =>
                  handleChannelChange(e.target.value as NotificationChannel | "")
                }
              >
                <option value="">Todos</option>
                {(Object.keys(NOTIFICATION_CHANNEL_LABEL) as NotificationChannel[]).map(
                  (c) => (
                    <option key={c} value={c}>
                      {NOTIFICATION_CHANNEL_LABEL[c]}
                    </option>
                  ),
                )}
              </select>
            </label>
          </div>

          <AsyncState
            isLoading={logsQuery.isLoading}
            isError={logsQuery.isError}
            error={getApiErrorMessage(logsQuery.error)}
            isEmpty={logsQuery.data ? logsQuery.data.content.length === 0 : false}
            emptyMessage="Nenhum log de notificação encontrado."
            onRetry={() => logsQuery.refetch()}
          >
            {logsQuery.data && logsQuery.data.content.length > 0 ? (
              <div className="space-y-4">
                <div className="overflow-x-auto">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Data</th>
                        <th>Canal</th>
                        <th>Tipo</th>
                        <th>Destinatário</th>
                        <th>Status</th>
                        <th>Erro</th>
                      </tr>
                    </thead>
                    <tbody>
                      {logsQuery.data.content.map((log) => (
                        <tr key={log.id}>
                          <td className="whitespace-nowrap">
                            {formatDateTime(log.createdAt)}
                          </td>
                          <td>{NOTIFICATION_CHANNEL_LABEL[log.channel]}</td>
                          <td>{NOTIFICATION_TYPE_LABEL[log.type]}</td>
                          <td>{log.recipient}</td>
                          <td>
                            <span
                              className={cn(
                                "badge",
                                LOG_STATUS_BADGE_CLASS[log.status],
                              )}
                            >
                              {NOTIFICATION_STATUS_LABEL[log.status]}
                            </span>
                          </td>
                          <td>
                            {log.errorMessage ? (
                              <span
                                title={log.errorMessage}
                                className="block max-w-56 truncate text-error"
                              >
                                {log.errorMessage}
                              </span>
                            ) : (
                              <span className="text-base-content/40">—</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                <Pagination
                  page={logsQuery.data.number + 1}
                  totalPages={logsQuery.data.totalPages}
                  totalElements={logsQuery.data.totalElements}
                  size={logsQuery.data.size}
                  onChange={setPage}
                />
              </div>
            ) : null}
          </AsyncState>
        </div>
      </div>
    </div>
  );
}
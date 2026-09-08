import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { AsyncState } from "~/components/ui/async-state";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";
import { PageHeader } from "~/components/ui/page-header";
import { getApiErrorMessage } from "~/lib/api";
import {
  formatCurrency,
  formatDate,
  ORDER_SHIFT_FIELDS,
  UNIT_MEASURE_LABEL,
} from "~/lib/utils";
import { getDailyProduction, sendDailyReport } from "~/services/report.service";
import { getConfig, updateConfig } from "~/services/notification.service";

function todayLocal(): string {
  const d = new Date();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${month}-${day}`;
}

function formatQuantity(value: number): string {
  return value.toLocaleString("pt-BR", { maximumFractionDigits: 2 });
}

export default function ProductionReportPage() {
  const [date, setDate] = useState<string>(() => todayLocal());
  const [reportTime, setReportTime] = useState("");
  const queryClient = useQueryClient();

  const reportQuery = useQuery({
    queryKey: ["report-production", date],
    queryFn: () => getDailyProduction(date),
  });

  const sendMutation = useMutation({
    mutationFn: () => sendDailyReport(date),
  });

  const configQuery = useQuery({
    queryKey: ["notifications-config"],
    queryFn: getConfig,
  });

  // Pré-preenche o horário salvo (formato "HH:mm" para o input time).
  useEffect(() => {
    const time = configQuery.data?.dailyReportTime;
    if (time) setReportTime(time.slice(0, 5));
  }, [configQuery.data]);

  const saveTimeMutation = useMutation({
    mutationFn: () =>
      updateConfig({
        ...configQuery.data,
        dailyReportTime: `${reportTime}:00`,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications-config"] });
    },
  });

  const dailyReportEnabled =
    configQuery.data?.dailyReportEnabled ?? true;

  const toggleReportEnabledMutation = useMutation({
    mutationFn: () =>
      updateConfig({
        ...configQuery.data,
        dailyReportEnabled: !dailyReportEnabled,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["notifications-config"] });
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Relatório de produção"
        subtitle="Consolidação diária de quantidades por produto e turno"
        actions={
          <button
            type="button"
            className="btn btn-primary"
            disabled={sendMutation.isPending}
            onClick={() => sendMutation.mutate()}
          >
            {sendMutation.isPending ? (
              <span
                className="loading loading-spinner loading-sm"
                aria-hidden="true"
              />
            ) : null}
            Enviar relatório
          </button>
        }
      />

      <div className="flex flex-wrap items-end gap-3">
        <FormField label="Data">
          <input
            type="date"
            className="input w-full sm:w-auto"
            value={date}
            max={todayLocal()}
            onChange={(e) => {
              const next = e.target.value || todayLocal();
              if (next !== date) sendMutation.reset();
              setDate(next);
            }}
          />
        </FormField>
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => {
            sendMutation.reset();
            setDate(todayLocal());
          }}
        >
          Hoje
        </button>
      </div>

      <div className="card border border-base-300 bg-base-100">
        <div className="card-body">
          <h2 className="card-title">Envio automático</h2>
          <p className="text-sm text-base-content/70">
            Define o horário em que o relatório diário é enviado
            automaticamente.
          </p>
          <div className="flex items-center justify-between gap-3 rounded-box border border-base-300 bg-base-200/50 p-3">
            <div>
              <p className="font-medium">Enviar automaticamente</p>
              <p className="text-sm text-base-content/70">
                Envia o relatório diário de produção ao admin no horário
                definido abaixo.
              </p>
            </div>
            <label className="flex cursor-pointer items-center gap-2">
              <input
                type="checkbox"
                className="toggle toggle-primary"
                checked={dailyReportEnabled}
                disabled={toggleReportEnabledMutation.isPending}
                onChange={() => toggleReportEnabledMutation.mutate()}
              />
            </label>
          </div>
          <div className="flex flex-wrap items-end gap-3">
            <FormField label="Horário do envio diário">
              <input
                type="time"
                className="input w-full sm:w-auto"
                value={reportTime}
                onChange={(e) => setReportTime(e.target.value)}
              />
            </FormField>
            <button
              type="button"
              className="btn btn-outline"
              disabled={saveTimeMutation.isPending || !reportTime}
              onClick={() => saveTimeMutation.mutate()}
            >
              {saveTimeMutation.isPending ? (
                <span
                  className="loading loading-spinner loading-sm"
                  aria-hidden="true"
                />
              ) : null}
              Salvar horário
            </button>
          </div>

          {saveTimeMutation.isSuccess ? (
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
              <span className="flex-1">Horário de envio salvo.</span>
            </div>
          ) : null}

          {saveTimeMutation.isError ? (
            <ErrorAlert error={getApiErrorMessage(saveTimeMutation.error)} />
          ) : null}

          {toggleReportEnabledMutation.isSuccess ? (
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
                {dailyReportEnabled
                  ? "Envio automático ativado."
                  : "Envio automático desativado."}
              </span>
            </div>
          ) : null}

          {toggleReportEnabledMutation.isError ? (
            <ErrorAlert
              error={getApiErrorMessage(toggleReportEnabledMutation.error)}
            />
          ) : null}
        </div>
      </div>

      {sendMutation.isSuccess && sendMutation.data ? (
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
          <div className="flex-1">
            <p className="font-medium">Relatório enviado com sucesso.</p>
            <p className="text-sm">
              WhatsApp: {sendMutation.data.whatsappSent ? "enviado" : "não enviado"}
              {sendMutation.data.whatsappMessage
                ? ` — ${sendMutation.data.whatsappMessage}`
                : ""}
            </p>
          </div>
        </div>
      ) : null}

      {sendMutation.isError ? (
        <ErrorAlert error={getApiErrorMessage(sendMutation.error)} />
      ) : null}

      <AsyncState
        isLoading={reportQuery.isLoading}
        isError={reportQuery.isError}
        error={getApiErrorMessage(reportQuery.error)}
        isEmpty={reportQuery.data ? reportQuery.data.items.length === 0 : false}
        emptyMessage="Sem produção nesta data."
        onRetry={() => reportQuery.refetch()}
      >
        {reportQuery.data ? (
          <div className="card bg-base-100 shadow">
            <div className="card-body">
              <h2 className="card-title">
                Produção de {formatDate(reportQuery.data.date)}
              </h2>

              {reportQuery.data.vendorNames.length > 0 ? (
                <p className="text-sm text-base-content/70">
                  Pedido(s) de: {reportQuery.data.vendorNames.join(", ")}
                </p>
              ) : null}

              <div className="overflow-x-auto">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Produto</th>
                      <th>Unidade</th>
                      {ORDER_SHIFT_FIELDS.map((shift) => (
                        <th key={shift.key} className="text-right">
                          {shift.label}
                        </th>
                      ))}
                      <th className="text-right">Quantidade total</th>
                      <th className="text-right">Valor total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reportQuery.data.items.map((item) => (
                      <tr key={item.productId}>
                        <td className="font-medium">{item.productName}</td>
                        <td>{UNIT_MEASURE_LABEL[item.unitMeasure]}</td>
                        {ORDER_SHIFT_FIELDS.map((shift) => (
                          <td key={shift.key} className="text-right">
                            {formatQuantity(item[shift.key])}
                          </td>
                        ))}
                        <td className="text-right">
                          {formatQuantity(item.totalQuantity)}
                        </td>
                        <td className="text-right">
                          {formatCurrency(item.totalAmount)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr>
                      <th colSpan={6} className="text-right font-bold">
                        TOTAL
                      </th>
                      <th className="text-right font-bold">
                        {formatCurrency(reportQuery.data.totalAmount)}
                      </th>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          </div>
        ) : null}
      </AsyncState>
    </div>
  );
}
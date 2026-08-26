import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";

import { AsyncState } from "~/components/ui/async-state";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";
import { PageHeader } from "~/components/ui/page-header";
import { useAuth } from "~/hooks/use-auth";
import { getApiErrorMessage } from "~/lib/api";
import { formatCurrency, formatDate, UNIT_MEASURE_LABEL } from "~/lib/utils";
import { getDailyProduction, sendDailyReport } from "~/services/report.service";

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
  const { isAdmin } = useAuth();
  const [date, setDate] = useState<string>(() => todayLocal());

  const reportQuery = useQuery({
    queryKey: ["report-production", date],
    queryFn: () => getDailyProduction(date),
  });

  const sendMutation = useMutation({
    mutationFn: () => sendDailyReport(date),
  });

  return (
    <div className="space-y-6">
      <PageHeader
        title="Relatório de produção"
        subtitle="Consolidação diária de quantidades por produto"
        actions={
          isAdmin ? (
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
          ) : undefined
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
              {" — "}
              E-mail: {sendMutation.data.emailSent ? "enviado" : "não enviado"}
            </p>
            {sendMutation.data.whatsappMessage || sendMutation.data.emailMessage ? (
              <p className="mt-1 text-sm text-base-content/70">
                {sendMutation.data.whatsappMessage
                  ? `WhatsApp: ${sendMutation.data.whatsappMessage}`
                  : ""}
                {sendMutation.data.whatsappMessage && sendMutation.data.emailMessage
                  ? " · "
                  : ""}
                {sendMutation.data.emailMessage
                  ? `E-mail: ${sendMutation.data.emailMessage}`
                  : ""}
              </p>
            ) : null}
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
              <div className="overflow-x-auto">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Produto</th>
                      <th>Unidade</th>
                      <th className="text-right">Quantidade total</th>
                      <th className="text-right">Valor total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {reportQuery.data.items.map((item) => (
                      <tr key={item.productId}>
                        <td className="font-medium">{item.productName}</td>
                        <td>{UNIT_MEASURE_LABEL[item.unitMeasure]}</td>
                        <td className="text-right">
                          {formatQuantity(item.totalQuantity)}
                        </td>
                        <td className="text-right">{formatCurrency(item.totalAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr>
                      <th colSpan={3} className="text-right font-bold">
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
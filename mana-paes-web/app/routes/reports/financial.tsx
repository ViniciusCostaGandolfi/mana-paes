import { useState } from "react";
import { useQuery } from "@tanstack/react-query";

import { AsyncState } from "~/components/ui/async-state";
import { FormField } from "~/components/ui/form-field";
import { PageHeader } from "~/components/ui/page-header";
import { getApiErrorMessage } from "~/lib/api";
import { formatCurrency, formatDate } from "~/lib/utils";
import { getDailyFinancial } from "~/services/report.service";

function todayLocal(): string {
  const d = new Date();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${month}-${day}`;
}

export default function FinancialReportPage() {
  const [date, setDate] = useState<string>(() => todayLocal());

  const reportQuery = useQuery({
    queryKey: ["report-financial", date],
    queryFn: () => getDailyFinancial(date),
  });

  return (
    <div className="space-y-6">
      <PageHeader title="Relatório financeiro" subtitle="Faturamento diário" />

      <div className="flex flex-wrap items-end gap-3">
        <FormField label="Data">
          <input
            type="date"
            className="input w-full sm:w-auto"
            value={date}
            max={todayLocal()}
            onChange={(e) => setDate(e.target.value || todayLocal())}
          />
        </FormField>
        <button
          type="button"
          className="btn btn-outline"
          onClick={() => setDate(todayLocal())}
        >
          Hoje
        </button>
      </div>

      <AsyncState
        isLoading={reportQuery.isLoading}
        isError={reportQuery.isError}
        error={getApiErrorMessage(reportQuery.error)}
        onRetry={() => reportQuery.refetch()}
      >
        {reportQuery.data ? (
          <div className="card bg-base-100 shadow">
            <div className="card-body">
              <h2 className="card-title">
                Faturamento de {formatDate(reportQuery.data.date)}
              </h2>
              <div className="stats stats-vertical w-full sm:stats-horizontal">
                <div className="stat">
                  <div className="stat-title">Pedidos no dia</div>
                  <div className="stat-value">{reportQuery.data.totalOrders}</div>
                </div>
                <div className="stat">
                  <div className="stat-title">Faturamento</div>
                  <div className="stat-value">
                    {formatCurrency(reportQuery.data.totalAmount)}
                  </div>
                </div>
              </div>
            </div>
          </div>
        ) : null}
      </AsyncState>
    </div>
  );
}
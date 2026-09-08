import { useState } from "react";
import { Link } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AsyncState } from "~/components/ui/async-state";
import { ErrorAlert } from "~/components/ui/error-alert";
import { PageHeader } from "~/components/ui/page-header";
import { Pagination } from "~/components/ui/pagination";
import { StatusBadge } from "~/components/ui/status-badge";
import { useAuth } from "~/hooks/use-auth";
import { getApiErrorMessage } from "~/lib/api";
import { formatCurrency, formatDate, formatDateTime } from "~/lib/utils";
import { acceptAllOrders, listOrders } from "~/services/order.service";
import type { OrderStatus } from "~/types/api";

const PAGE_SIZE = 20;

export default function OrdersPage() {
  const { isAdmin, isSysAdmin } = useAuth();
  const [page, setPage] = useState(1);
  const queryClient = useQueryClient();

  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["orders", page],
    queryFn: () => listOrders({ page: page - 1, size: PAGE_SIZE }),
  });

  const acceptAllMutation = useMutation({
    mutationFn: acceptAllOrders,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });

  const orders = data?.content ?? [];
  const totalPages = data?.page.totalPages ?? 0;
  const totalElements = data?.page.totalElements ?? 0;

  // Ressalta visualmente pedidos que precisam de atenção.
  const rowHighlightClass = (status: OrderStatus): string => {
    if (status === "PENDING") return "bg-warning/10";
    if (status === "ACCEPTED") return "bg-success/10";
    return "";
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Pedidos"
        subtitle="Acompanhe todos os pedidos do estabelecimento."
        actions={
          <div className="flex flex-wrap items-center gap-2">
            {isAdmin && !isSysAdmin ? (
              <button
                type="button"
                className="btn btn-success"
                disabled={acceptAllMutation.isPending}
                onClick={() => acceptAllMutation.mutate()}
              >
                {acceptAllMutation.isPending ? (
                  <span
                    className="loading loading-spinner loading-sm"
                    aria-hidden="true"
                  />
                ) : null}
                Aceitar todos
              </button>
            ) : null}
            <Link to="/orders/new" className="btn btn-primary">
              Novo pedido
            </Link>
          </div>
        }
      />

      {acceptAllMutation.isSuccess && acceptAllMutation.data ? (
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
          <span className="flex-1">{acceptAllMutation.data.message}</span>
        </div>
      ) : null}

      {acceptAllMutation.isError ? (
        <ErrorAlert error={getApiErrorMessage(acceptAllMutation.error)} />
      ) : null}

      <AsyncState
        isLoading={isLoading}
        isError={isError}
        error={error ? getApiErrorMessage(error) : undefined}
        onRetry={() => refetch()}
        isEmpty={orders.length === 0}
        emptyMessage="Nenhum pedido encontrado."
        emptyHint="Os pedidos enviados pelos vendedores aparecerão aqui."
      >
        <div className="card border border-base-300 bg-base-100">
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>Pedido</th>
                  <th>Vendedor</th>
                  <th>Entrega</th>
                  <th>Criado em</th>
                  <th>Status</th>
                  <th className="text-right">Total</th>
                  <th className="text-right">Ações</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id} className={rowHighlightClass(order.status)}>
                    <td className="font-mono text-sm" title={order.id}>
                      {order.id.slice(0, 8)}
                    </td>
                    <td>{order.vendorName ?? order.requesterName ?? "—"}</td>
                    <td>{formatDate(order.deliveryDate)}</td>
                    <td>{formatDateTime(order.createdAt)}</td>
                    <td>
                      <StatusBadge status={order.status} />
                    </td>
                    <td className="text-right font-medium">
                      {formatCurrency(order.totalAmount)}
                    </td>
                    <td className="text-right">
                      <Link to={`/orders/${order.id}`} className="link link-primary">
                        Ver
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </AsyncState>

      {totalElements > 0 ? (
        <Pagination
          page={page}
          totalPages={totalPages}
          totalElements={totalElements}
          size={PAGE_SIZE}
          onChange={setPage}
        />
      ) : null}
    </div>
  );
}
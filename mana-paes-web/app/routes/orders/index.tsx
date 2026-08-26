import { useState } from "react";
import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { AsyncState } from "~/components/ui/async-state";
import { PageHeader } from "~/components/ui/page-header";
import { Pagination } from "~/components/ui/pagination";
import { StatusBadge } from "~/components/ui/status-badge";
import { useAuth } from "~/hooks/use-auth";
import { getApiErrorMessage } from "~/lib/api";
import { formatCurrency, formatDate, formatDateTime } from "~/lib/utils";
import { listOrders } from "~/services/order.service";
import type { OrderStatus } from "~/types/api";

const PAGE_SIZE = 20;

export default function OrdersPage() {
  const { isRequester, isAdmin, isProduction } = useAuth();
  const [page, setPage] = useState(1);

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

  const orders = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const totalElements = data?.totalElements ?? 0;

  const subtitle = isRequester
    ? "Acompanhe aqui os seus pedidos."
    : isProduction
      ? "Acompanhe e atualize o status de produção dos pedidos."
      : "Acompanhe todos os pedidos do estabelecimento.";

  // Para PRODUÇÃO, ressalta visualmente pedidos que precisam de atenção.
  const rowHighlightClass = (status: OrderStatus): string => {
    if (!isProduction) return "";
    if (status === "PENDING") return "bg-warning/10";
    if (status === "IN_PRODUCTION") return "bg-info/10";
    return "";
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Pedidos"
        subtitle={subtitle}
        actions={
          isRequester || isAdmin ? (
            <Link to="/orders/new" className="btn btn-primary">
              Novo pedido
            </Link>
          ) : undefined
        }
      />

      <AsyncState
        isLoading={isLoading}
        isError={isError}
        error={error ? getApiErrorMessage(error) : undefined}
        onRetry={() => refetch()}
        isEmpty={orders.length === 0}
        emptyMessage="Nenhum pedido encontrado."
        emptyHint={
          isRequester
            ? 'Crie seu primeiro pedido pelo botão "Novo pedido".'
            : undefined
        }
      >
        <div className="card border border-base-300 bg-base-100">
          <div className="overflow-x-auto">
            <table className="table">
              <thead>
                <tr>
                  <th>Pedido</th>
                  {!isRequester ? <th>Solicitante</th> : null}
                  <th>Entrega</th>
                  <th>Criado em</th>
                  <th>Status</th>
                  <th className="text-right">Total</th>
                  <th className="text-right">Ações</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr
                    key={order.id}
                    className={rowHighlightClass(order.status)}
                  >
                    <td className="font-mono text-sm" title={order.id}>
                      {order.id.slice(0, 8)}
                    </td>
                    {!isRequester ? <td>{order.requesterName}</td> : null}
                    <td>{formatDate(order.deliveryDate)}</td>
                    <td>{formatDateTime(order.createdAt)}</td>
                    <td>
                      <StatusBadge status={order.status} />
                    </td>
                    <td className="text-right font-medium">
                      {formatCurrency(order.totalAmount)}
                    </td>
                    <td className="text-right">
                      <Link
                        to={`/orders/${order.id}`}
                        className="link link-primary"
                      >
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
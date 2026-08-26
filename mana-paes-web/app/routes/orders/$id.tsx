import { useState } from "react";
import { useParams } from "react-router";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AsyncState } from "~/components/ui/async-state";
import { ConfirmDialog } from "~/components/ui/confirm-dialog";
import { ErrorAlert } from "~/components/ui/error-alert";
import { PageHeader } from "~/components/ui/page-header";
import { StatusBadge } from "~/components/ui/status-badge";
import { useAuth } from "~/hooks/use-auth";
import { getApiErrorMessage } from "~/lib/api";
import {
  formatCurrency,
  formatDate,
  formatDateTime,
  ORDER_STATUS_LABEL,
  UNIT_MEASURE_LABEL,
} from "~/lib/utils";
import { getOrder, updateOrderStatus } from "~/services/order.service";
import type { OrderStatus, OrderStatusUpdateRequest } from "~/types/api";

const STATUS_FLOW: OrderStatus[] = [
  "PENDING",
  "IN_PRODUCTION",
  "READY",
  "DELIVERED",
];

function formatQuantity(value: number): string {
  return new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 }).format(
    value,
  );
}

export default function OrderDetailPage() {
  const { id } = useParams();
  const { isAdmin, isProduction } = useAuth();
  const queryClient = useQueryClient();
  const [isCancelDialogOpen, setIsCancelDialogOpen] = useState(false);

  const orderId = id ?? "";

  const {
    data: order,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["order", orderId],
    queryFn: () => getOrder(orderId),
    enabled: !!orderId,
  });

  const statusMutation = useMutation({
    mutationFn: (body: OrderStatusUpdateRequest) =>
      updateOrderStatus(orderId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["order", orderId] });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      setIsCancelDialogOpen(false);
    },
  });

  const errorMessage = error ? getApiErrorMessage(error) : undefined;
  const idShort = orderId.slice(0, 8) || "—";

  const canManageStatus = (isAdmin || isProduction) && !!order;
  const currentIndex = order ? STATUS_FLOW.indexOf(order.status) : -1;
  const nextStatus =
    currentIndex >= 0 && currentIndex < STATUS_FLOW.length - 1
      ? STATUS_FLOW[currentIndex + 1]
      : null;
  const canCancel =
    !!order && order.status !== "DELIVERED" && order.status !== "CANCELLED";

  const handleAdvance = () => {
    if (!nextStatus || !order) return;
    statusMutation.mutate({ status: nextStatus });
  };

  const handleCancel = () => {
    if (!order) return;
    statusMutation.mutate({ status: "CANCELLED" });
  };

  if (!orderId) {
    return (
      <div className="space-y-6">
        <PageHeader title="Pedido" />
        <ErrorAlert error="Identificador do pedido não encontrado." />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Pedido #${idShort}`}
        actions={
          <div className="flex flex-wrap items-center gap-2">
            {order ? <StatusBadge status={order.status} /> : null}
            {canManageStatus && nextStatus ? (
              <button
                type="button"
                className="btn btn-primary btn-sm"
                onClick={handleAdvance}
                disabled={statusMutation.isPending}
              >
                {statusMutation.isPending ? (
                  <span
                    className="loading loading-spinner loading-xs"
                    aria-hidden="true"
                  />
                ) : null}
                Avançar para {ORDER_STATUS_LABEL[nextStatus]}
              </button>
            ) : null}
            {canManageStatus && canCancel ? (
              <button
                type="button"
                className="btn btn-outline btn-error btn-sm"
                onClick={() => setIsCancelDialogOpen(true)}
                disabled={statusMutation.isPending}
              >
                Cancelar pedido
              </button>
            ) : null}
          </div>
        }
      />

      {statusMutation.isError ? (
        <ErrorAlert error={getApiErrorMessage(statusMutation.error)} />
      ) : null}

      <AsyncState
        isLoading={isLoading}
        isError={isError}
        error={errorMessage}
        onRetry={() => refetch()}
      >
        {order ? (
          <>
            <div className="card border border-base-300 bg-base-100">
              <div className="card-body">
                <h2 className="card-title">Resumo do pedido</h2>
                <div className="overflow-x-auto">
                  <table className="table">
                    <tbody>
                      <tr>
                        <th className="w-44">ID</th>
                        <td className="font-mono text-sm">{order.id}</td>
                      </tr>
                      <tr>
                        <th>Solicitante</th>
                        <td>{order.requesterName}</td>
                      </tr>
                      <tr>
                        <th>Data de entrega</th>
                        <td>{formatDate(order.deliveryDate)}</td>
                      </tr>
                      <tr>
                        <th>Criado em</th>
                        <td>{formatDateTime(order.createdAt)}</td>
                      </tr>
                      <tr>
                        <th>Status</th>
                        <td>
                          <StatusBadge status={order.status} />
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </div>

            <div className="card border border-base-300 bg-base-100">
              <div className="card-body">
                <h2 className="card-title">Itens</h2>
                <div className="overflow-x-auto">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Produto</th>
                        <th>Unidade</th>
                        <th className="text-right">Quantidade</th>
                        <th className="text-right">Preço unitário</th>
                        <th className="text-right">Subtotal</th>
                      </tr>
                    </thead>
                    <tbody>
                      {order.items.map((item) => (
                        <tr key={item.productId}>
                          <td>{item.productName}</td>
                          <td>{UNIT_MEASURE_LABEL[item.unitMeasure]}</td>
                          <td className="text-right">
                            {formatQuantity(item.quantity)}
                          </td>
                          <td className="text-right">
                            {formatCurrency(item.unitPrice)}
                          </td>
                          <td className="text-right">
                            {formatCurrency(item.subtotal)}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                    <tfoot>
                      <tr>
                        <th colSpan={4}>Total</th>
                        <th className="text-right font-bold">
                          {formatCurrency(order.totalAmount)}
                        </th>
                      </tr>
                    </tfoot>
                  </table>
                </div>
              </div>
            </div>
          </>
        ) : null}
      </AsyncState>

      <ConfirmDialog
        open={isCancelDialogOpen}
        title="Cancelar pedido"
        message={`Deseja realmente cancelar o pedido #${idShort}? Esta ação não pode ser desfeita.`}
        confirmLabel="Cancelar pedido"
        cancelLabel="Voltar"
        danger
        isLoading={statusMutation.isPending}
        onConfirm={handleCancel}
        onCancel={() => setIsCancelDialogOpen(false)}
      />
    </div>
  );
}
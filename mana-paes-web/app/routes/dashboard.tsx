import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "~/hooks/use-auth";
import { listOrders } from "~/services/order.service";
import { listProducts } from "~/services/product.service";
import { listUsers } from "~/services/user.service";
import { getDailyFinancial } from "~/services/report.service";
import type { OrderResponse, OrderStatus } from "~/types/api";
import { cn, formatCurrency, formatDate } from "~/lib/utils";
import { AsyncState } from "~/components/ui/async-state";
import { EmptyState } from "~/components/ui/empty-state";
import { PageHeader } from "~/components/ui/page-header";
import { StatusBadge } from "~/components/ui/status-badge";

function sortByCreatedAtDesc(orders: OrderResponse[]): OrderResponse[] {
  return [...orders].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

const NEEDS_ACTION: OrderStatus[] = ["PENDING", "IN_PRODUCTION"];

function RequesterDashboard() {
  const ordersQuery = useQuery({
    queryKey: ["orders", 0],
    queryFn: () => listOrders({ page: 0, size: 100 }),
  });

  const orders = ordersQuery.data?.content ?? [];
  const openCount = orders.filter((order) =>
    NEEDS_ACTION.includes(order.status),
  ).length;
  const totalInvested = orders.reduce((sum, order) => sum + order.totalAmount, 0);
  const recent = sortByCreatedAtDesc(orders).slice(0, 5);

  return (
    <AsyncState
      isLoading={ordersQuery.isLoading}
      isError={ordersQuery.isError}
      error={ordersQuery.error}
      isEmpty={orders.length === 0}
      emptyMessage="Nenhum pedido encontrado."
      emptyHint="Crie seu primeiro pedido para começar."
      onRetry={() => ordersQuery.refetch()}
    >
      <div className="space-y-6">
        <div className="stats stats-vertical w-full shadow sm:stats-horizontal">
          <div className="stat">
            <div className="stat-title">Meus pedidos</div>
            <div className="stat-value">
              {ordersQuery.data?.totalElements ?? orders.length}
            </div>
            <div className="stat-desc">Pedidos realizados</div>
          </div>
          <div className="stat">
            <div className="stat-title">Em aberto</div>
            <div className="stat-value">{openCount}</div>
            <div className="stat-desc">Pendentes ou em produção</div>
          </div>
          <div className="stat">
            <div className="stat-title">Total investido</div>
            <div className="stat-value text-2xl">{formatCurrency(totalInvested)}</div>
            <div className="stat-desc">Soma de todos os pedidos</div>
          </div>
        </div>

        <div className="card bg-base-100 shadow">
          <div className="card-body">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
              <h2 className="card-title">Pedidos recentes</h2>
              <Link to="/orders" className="link link-hover link-primary text-sm">
                Ver todos
              </Link>
            </div>
            {recent.length === 0 ? (
              <EmptyState message="Nenhum pedido encontrado." />
            ) : (
              <div className="overflow-x-auto">
                <table className="table table-sm">
                  <thead>
                    <tr>
                      <th>Entrega</th>
                      <th>Status</th>
                      <th className="text-right">Total</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recent.map((order) => (
                      <tr key={order.id}>
                        <td>{formatDate(order.deliveryDate)}</td>
                        <td>
                          <StatusBadge status={order.status} />
                        </td>
                        <td className="text-right font-medium">
                          {formatCurrency(order.totalAmount)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </AsyncState>
  );
}

function ProductionDashboard() {
  const ordersQuery = useQuery({
    queryKey: ["orders", 0],
    queryFn: () => listOrders({ page: 0, size: 100 }),
  });

  const orders = ordersQuery.data?.content ?? [];
  const countByStatus = (status: OrderStatus) =>
    orders.filter((order) => order.status === status).length;
  const pending = countByStatus("PENDING");
  const inProduction = countByStatus("IN_PRODUCTION");
  const ready = countByStatus("READY");
  const recent = sortByCreatedAtDesc(orders).slice(0, 5);

  return (
    <AsyncState
      isLoading={ordersQuery.isLoading}
      isError={ordersQuery.isError}
      error={ordersQuery.error}
      isEmpty={orders.length === 0}
      emptyMessage="Nenhum pedido encontrado."
      emptyHint="Os pedidos recebidos aparecerão aqui."
      onRetry={() => ordersQuery.refetch()}
    >
      <div className="space-y-6">
        <div className="stats stats-vertical w-full shadow sm:stats-horizontal">
          <div className="stat">
            <div className="stat-title">Pendentes</div>
            <div className="stat-value">{pending}</div>
            <div className="stat-desc">Aguardando produção</div>
          </div>
          <div className="stat">
            <div className="stat-title">Em produção</div>
            <div className="stat-value">{inProduction}</div>
            <div className="stat-desc">Sendo preparados</div>
          </div>
          <div className="stat">
            <div className="stat-title">Prontos</div>
            <div className="stat-value">{ready}</div>
            <div className="stat-desc">Aguardando entrega</div>
          </div>
        </div>

        <div className="card bg-base-100 shadow">
          <div className="card-body">
            <h2 className="card-title">Pedidos recentes</h2>
            {recent.length === 0 ? (
              <EmptyState message="Nenhum pedido encontrado." />
            ) : (
              <div className="overflow-x-auto">
                <table className="table table-sm">
                  <thead>
                    <tr>
                      <th>Entrega</th>
                      <th>Status</th>
                      <th className="text-right">Total</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {recent.map((order) => {
                      const needsAction = NEEDS_ACTION.includes(order.status);
                      return (
                        <tr
                          key={order.id}
                          className={cn(needsAction && "bg-warning/10")}
                        >
                          <td>{formatDate(order.deliveryDate)}</td>
                          <td>
                            <StatusBadge status={order.status} />
                          </td>
                          <td className="text-right font-medium">
                            {formatCurrency(order.totalAmount)}
                          </td>
                          <td className="text-right">
                            <Link
                              to={`/orders/${order.id}`}
                              className="link link-hover link-primary text-sm"
                            >
                              Detalhes
                            </Link>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </AsyncState>
  );
}

function AdminDashboard() {
  const today = new Date().toISOString().slice(0, 10);

  const ordersQuery = useQuery({
    queryKey: ["orders", 0],
    queryFn: () => listOrders({ page: 0, size: 100 }),
  });
  const financialQuery = useQuery({
    queryKey: ["reports", "daily-financial", today],
    queryFn: () => getDailyFinancial(today),
  });
  const usersQuery = useQuery({
    queryKey: ["users", 0],
    queryFn: () => listUsers({ page: 0, size: 1 }),
  });
  const productsQuery = useQuery({
    queryKey: ["products", "active", 0],
    queryFn: () => listProducts({ active: true, page: 0, size: 1 }),
  });

  const orders = ordersQuery.data?.content ?? [];
  const ordersToday = orders.filter((order) => order.deliveryDate === today).length;
  const revenueToday = financialQuery.data?.totalAmount ?? 0;
  const requesters = usersQuery.data?.totalElements ?? 0;
  const activeProducts = productsQuery.data?.totalElements ?? 0;
  const recent = sortByCreatedAtDesc(orders).slice(0, 5);

  const onRetry = () => {
    ordersQuery.refetch();
    financialQuery.refetch();
    usersQuery.refetch();
    productsQuery.refetch();
  };

  return (
    <AsyncState
      isLoading={
        ordersQuery.isLoading ||
        financialQuery.isLoading ||
        usersQuery.isLoading ||
        productsQuery.isLoading
      }
      isError={
        ordersQuery.isError ||
        financialQuery.isError ||
        usersQuery.isError ||
        productsQuery.isError
      }
      error={
        ordersQuery.error ??
        financialQuery.error ??
        usersQuery.error ??
        productsQuery.error
      }
      isEmpty={orders.length === 0}
      emptyMessage="Nenhum pedido encontrado."
      emptyHint="Os pedidos realizados pelos solicitantes aparecerão aqui."
      onRetry={onRetry}
    >
      <div className="space-y-6">
        <div className="stats stats-vertical w-full shadow lg:stats-horizontal">
          <div className="stat">
            <div className="stat-title">Pedidos hoje</div>
            <div className="stat-value">{ordersToday}</div>
            <div className="stat-desc">Entrega prevista para hoje</div>
          </div>
          <div className="stat">
            <div className="stat-title">Faturamento hoje</div>
            <div className="stat-value text-2xl">{formatCurrency(revenueToday)}</div>
            <div className="stat-desc">Total dos pedidos de hoje</div>
          </div>
          <div className="stat">
            <div className="stat-title">Solicitantes</div>
            <div className="stat-value">{requesters}</div>
            <div className="stat-desc">Usuários cadastrados</div>
          </div>
          <div className="stat">
            <div className="stat-title">Produtos ativos</div>
            <div className="stat-value">{activeProducts}</div>
            <div className="stat-desc">Disponíveis no catálogo</div>
          </div>
        </div>

        <div className="card bg-base-100 shadow">
          <div className="card-body">
            <h2 className="card-title">Últimos pedidos</h2>
            {recent.length === 0 ? (
              <EmptyState message="Nenhum pedido encontrado." />
            ) : (
              <div className="overflow-x-auto">
                <table className="table table-sm">
                  <thead>
                    <tr>
                      <th>Solicitante</th>
                      <th>Entrega</th>
                      <th>Status</th>
                      <th className="text-right">Total</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {recent.map((order) => (
                      <tr key={order.id}>
                        <td>{order.requesterName}</td>
                        <td>{formatDate(order.deliveryDate)}</td>
                        <td>
                          <StatusBadge status={order.status} />
                        </td>
                        <td className="text-right font-medium">
                          {formatCurrency(order.totalAmount)}
                        </td>
                        <td className="text-right">
                          <Link
                            to={`/orders/${order.id}`}
                            className="link link-hover link-primary text-sm"
                          >
                            Detalhes
                          </Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      </div>
    </AsyncState>
  );
}

export default function DashboardPage() {
  const { user, isRequester, isProduction, isAdmin } = useAuth();

  const actions =
    isRequester ? (
      <Link to="/orders/new" className="btn btn-primary btn-sm">
        Novo pedido
      </Link>
    ) : isAdmin ? (
      <>
        <Link to="/orders/new" className="btn btn-primary btn-sm">
          Novo pedido
        </Link>
        <Link to="/products" className="btn btn-outline btn-sm">
          Produtos
        </Link>
        <Link to="/reports/production" className="btn btn-outline btn-sm">
          Relatórios
        </Link>
      </>
    ) : undefined;

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" subtitle={user?.name} actions={actions} />
      {isRequester ? <RequesterDashboard /> : null}
      {isProduction ? <ProductionDashboard /> : null}
      {isAdmin ? <AdminDashboard /> : null}
    </div>
  );
}
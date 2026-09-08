import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "~/hooks/use-auth";
import { listOrders } from "~/services/order.service";
import { listProducts } from "~/services/product.service";
import { listUsers } from "~/services/user.service";
import { getDailyFinancial } from "~/services/report.service";
import type { OrderResponse } from "~/types/api";
import { formatCurrency, formatDate } from "~/lib/utils";
import { AsyncState } from "~/components/ui/async-state";
import { EmptyState } from "~/components/ui/empty-state";
import { PageHeader } from "~/components/ui/page-header";
import { StatusBadge } from "~/components/ui/status-badge";
import { WhatsappOnboardingBanner } from "~/components/ui/whatsapp-onboarding-banner";

function sortByCreatedAtDesc(orders: OrderResponse[]): OrderResponse[] {
  return [...orders].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
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
  const requesters = usersQuery.data?.page.totalElements ?? 0;
  const activeProducts = productsQuery.data?.page.totalElements ?? 0;
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
      emptyHint="Os pedidos realizados pelos clientes aparecerão aqui."
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
            <div className="stat-title">Usuários</div>
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
                      <th>Vendedor</th>
                      <th>Entrega</th>
                      <th>Status</th>
                      <th className="text-right">Total</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {recent.map((order) => (
                      <tr key={order.id}>
                        <td>{order.vendorName ?? order.requesterName ?? "—"}</td>
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
  const { user, isAdmin, isSysAdmin } = useAuth();

  const actions = isAdmin ? (
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
      {isSysAdmin ? <WhatsappOnboardingBanner /> : null}
      {isAdmin ? <AdminDashboard /> : null}
    </div>
  );
}
import { useState } from "react";
import { Link } from "react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { AsyncState } from "~/components/ui/async-state";
import { ConfirmDialog } from "~/components/ui/confirm-dialog";
import { ErrorAlert } from "~/components/ui/error-alert";
import { PageHeader } from "~/components/ui/page-header";
import { Pagination } from "~/components/ui/pagination";
import { getApiErrorMessage } from "~/lib/api";
import { queryClient } from "~/lib/query";
import { UNIT_MEASURE_LABEL, cn, formatCurrency } from "~/lib/utils";
import {
  listProducts,
  setProductActive,
} from "~/services/product.service";
import type { ProductResponse } from "~/types/api";

const PAGE_SIZE = 20;

type ActiveFilter = "" | "true" | "false";

export default function ProductsPage() {
  // `page` segue a convenção 0-based da API (PageResponse.number do backend).
  const [page, setPage] = useState(0);
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>("");
  const [target, setTarget] = useState<ProductResponse | null>(null);
  const [toggleError, setToggleError] = useState<string | null>(null);

  const active =
    activeFilter === "" ? undefined : activeFilter === "true";

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["products", { active, page }],
    queryFn: () => listProducts({ active, page, size: PAGE_SIZE }),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      setProductActive(id, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products"] });
      setTarget(null);
      setToggleError(null);
    },
    onError: (err) => {
      setToggleError(getApiErrorMessage(err));
      setTarget(null);
    },
  });

  const handleFilterChange = (value: ActiveFilter) => {
    setActiveFilter(value);
    setPage(0);
  };

  const handleConfirmToggle = () => {
    if (!target) return;
    toggleMutation.mutate({ id: target.id, active: !target.active });
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Produtos"
        actions={
          <Link to="/products/new" className="btn btn-primary">
            Novo produto
          </Link>
        }
      />

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <select
          className="select select-sm w-full sm:w-56"
          value={activeFilter}
          onChange={(event) =>
            handleFilterChange(event.target.value as ActiveFilter)
          }
          aria-label="Filtrar produtos por status"
        >
          <option value="">Todos</option>
          <option value="true">Ativos</option>
          <option value="false">Inativos</option>
        </select>
      </div>

      {toggleError ? (
        <ErrorAlert
          error={toggleError}
          onRetry={() => setToggleError(null)}
        />
      ) : null}

      <AsyncState
        isLoading={isLoading}
        isError={isError}
        error={error}
        onRetry={() => refetch()}
        isEmpty={!data || data.empty}
        emptyMessage="Nenhum produto encontrado."
        emptyHint="Cadastre um novo produto para começar."
      >
        {data && !data.empty ? (
          <div className="space-y-4">
            <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
              <table className="table">
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>Descrição</th>
                    <th>Unidade</th>
                    <th>Preço</th>
                    <th>Status</th>
                    <th className="text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((product) => (
                    <tr key={product.id}>
                      <td className="font-medium">{product.name}</td>
                      <td className="max-w-xs">
                        <span className="block truncate text-base-content/70">
                          {product.description || "—"}
                        </span>
                      </td>
                      <td>{UNIT_MEASURE_LABEL[product.unitMeasure]}</td>
                      <td>{formatCurrency(product.unitPrice)}</td>
                      <td>
                        <span
                          className={cn(
                            "badge",
                            product.active
                              ? "badge-success badge-outline"
                              : "badge-ghost",
                          )}
                        >
                          {product.active ? "Ativo" : "Inativo"}
                        </span>
                      </td>
                      <td>
                        <div className="flex justify-end gap-2">
                          <Link
                            to={`/products/${product.id}/edit`}
                            className="btn btn-sm btn-outline"
                          >
                            Editar
                          </Link>
                          <button
                            type="button"
                            className="btn btn-sm"
                            onClick={() => {
                              setToggleError(null);
                              setTarget(product);
                            }}
                          >
                            {product.active ? "Inativar" : "Ativar"}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <Pagination
              page={page + 1}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              size={data.size}
              onChange={(nextPage) => setPage(nextPage - 1)}
            />
          </div>
        ) : null}
      </AsyncState>

      <ConfirmDialog
        open={target !== null}
        title={target?.active ? "Inativar produto" : "Ativar produto"}
        message={
          target
            ? `Deseja ${target.active ? "inativar" : "ativar"} o produto "${target.name}"?`
            : ""
        }
        confirmLabel={target?.active ? "Inativar" : "Ativar"}
        onConfirm={handleConfirmToggle}
        onCancel={() => setTarget(null)}
        isLoading={toggleMutation.isPending}
        danger={target?.active ?? false}
      />
    </div>
  );
}
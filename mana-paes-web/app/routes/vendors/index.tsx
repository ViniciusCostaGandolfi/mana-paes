import { useState } from "react";
import { Link } from "react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { AsyncState } from "~/components/ui/async-state";
import { ConfirmDialog } from "~/components/ui/confirm-dialog";
import { ErrorAlert } from "~/components/ui/error-alert";
import { PageHeader } from "~/components/ui/page-header";
import { getApiErrorMessage } from "~/lib/api";
import { queryClient } from "~/lib/query";
import { cn, formatPhoneBR } from "~/lib/utils";
import { listVendors, setVendorActive } from "~/services/vendor.service";
import type { VendorResponse } from "~/types/api";

export default function VendorsPage() {
  const [target, setTarget] = useState<VendorResponse | null>(null);
  const [toggleError, setToggleError] = useState<string | null>(null);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["vendors"],
    queryFn: listVendors,
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      setVendorActive(id, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["vendors"] });
      setTarget(null);
      setToggleError(null);
    },
    onError: (err) => {
      setToggleError(getApiErrorMessage(err));
      setTarget(null);
    },
  });

  const handleConfirmToggle = () => {
    if (!target) return;
    toggleMutation.mutate({ id: target.id, active: !target.active });
  };

  const vendors = data ?? [];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Vendedores"
        subtitle="Vendedores fazem pedidos pelo link público, identificados pelo telefone."
        actions={
          <Link to="/vendors/new" className="btn btn-primary">
            Novo vendedor
          </Link>
        }
      />

      {toggleError ? (
        <ErrorAlert error={toggleError} onRetry={() => setToggleError(null)} />
      ) : null}

      <AsyncState
        isLoading={isLoading}
        isError={isError}
        error={error}
        onRetry={() => refetch()}
        isEmpty={vendors.length === 0}
        emptyMessage="Nenhum vendedor encontrado."
        emptyHint="Cadastre um vendedor para gerar o link de pedido."
      >
        <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Telefone</th>
                <th>WhatsApp</th>
                <th>Status</th>
                <th className="text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {vendors.map((vendor) => (
                <tr key={vendor.id}>
                  <td className="font-medium">{vendor.name}</td>
                  <td>{formatPhoneBR(vendor.phone)}</td>
                  <td>{formatPhoneBR(vendor.whatsappNumber) || "—"}</td>
                  <td>
                    <span
                      className={cn(
                        "badge",
                        vendor.active ? "badge-success badge-outline" : "badge-ghost",
                      )}
                    >
                      {vendor.active ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td>
                    <div className="flex justify-end gap-2">
                      <Link to={`/vendors/${vendor.id}/edit`} className="btn btn-sm btn-outline">
                        Editar
                      </Link>
                      <button
                        type="button"
                        className="btn btn-sm"
                        onClick={() => {
                          setToggleError(null);
                          setTarget(vendor);
                        }}
                      >
                        {vendor.active ? "Inativar" : "Ativar"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AsyncState>

      <ConfirmDialog
        open={target !== null}
        title={target?.active ? "Inativar vendedor" : "Ativar vendedor"}
        message={
          target
            ? `Deseja ${target.active ? "inativar" : "ativar"} o vendedor "${target.name}"?`
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
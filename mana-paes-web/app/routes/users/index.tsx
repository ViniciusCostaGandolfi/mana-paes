import { useState } from "react";
import { Link } from "react-router";
import { useMutation, useQuery } from "@tanstack/react-query";
import { AsyncState } from "~/components/ui/async-state";
import { ConfirmDialog } from "~/components/ui/confirm-dialog";
import { ErrorAlert } from "~/components/ui/error-alert";
import { PageHeader } from "~/components/ui/page-header";
import { Pagination } from "~/components/ui/pagination";
import { useAuth } from "~/hooks/use-auth";
import { getApiErrorMessage } from "~/lib/api";
import { queryClient } from "~/lib/query";
import { USER_ROLE_LABEL, cn } from "~/lib/utils";
import { listUsers, setUserActive } from "~/services/user.service";
import type { UserResponse, UserRole } from "~/types/api";

const PAGE_SIZE = 20;

const ROLE_BADGE_CLASS: Record<UserRole, string> = {
  ROLE_ADMIN: "badge-primary",
  ROLE_REQUESTER: "badge-info",
  ROLE_PRODUCTION: "badge-neutral",
};

export default function UsersPage() {
  const { user: currentUser } = useAuth();
  // `page` segue a convenção 0-based da API (PageResponse.number do backend).
  const [page, setPage] = useState(0);
  const [target, setTarget] = useState<UserResponse | null>(null);
  const [toggleError, setToggleError] = useState<string | null>(null);

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["users", page],
    queryFn: () => listUsers({ page, size: PAGE_SIZE }),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      setUserActive(id, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
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

  return (
    <div className="space-y-6">
      <PageHeader
        title="Usuários"
        actions={
          <Link to="/users/new" className="btn btn-primary">
            Novo usuário
          </Link>
        }
      />

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
        emptyMessage="Nenhum usuário encontrado."
        emptyHint="Cadastre um novo usuário para começar."
      >
        {data && !data.empty ? (
          <div className="space-y-4">
            <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
              <table className="table">
                <thead>
                  <tr>
                    <th>Nome</th>
                    <th>E-mail</th>
                    <th>Telefone</th>
                    <th>WhatsApp</th>
                    <th>Perfil</th>
                    <th>Status</th>
                    <th className="text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {data.content.map((user) => {
                    const isSelf = currentUser?.id === user.id;
                    return (
                      <tr key={user.id}>
                        <td className="font-medium">
                          {user.name}
                          {isSelf ? (
                            <span className="badge badge-ghost ml-2">
                              Você
                            </span>
                          ) : null}
                        </td>
                        <td>{user.email}</td>
                        <td>{user.phone || "—"}</td>
                        <td>{user.whatsappNumber || "—"}</td>
                        <td>
                          <span
                            className={cn(
                              "badge",
                              ROLE_BADGE_CLASS[user.role],
                            )}
                          >
                            {USER_ROLE_LABEL[user.role]}
                          </span>
                        </td>
                        <td>
                          <span
                            className={cn(
                              "badge",
                              user.active
                                ? "badge-success badge-outline"
                                : "badge-ghost",
                            )}
                          >
                            {user.active ? "Ativo" : "Inativo"}
                          </span>
                        </td>
                        <td>
                          <div className="flex justify-end gap-2">
                            <Link
                              to={`/users/${user.id}/edit`}
                              className="btn btn-sm btn-outline"
                            >
                              Editar
                            </Link>
                            {!isSelf ? (
                              <button
                                type="button"
                                className="btn btn-sm"
                                onClick={() => {
                                  setToggleError(null);
                                  setTarget(user);
                                }}
                              >
                                {user.active ? "Inativar" : "Ativar"}
                              </button>
                            ) : null}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
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
        title={target?.active ? "Inativar usuário" : "Ativar usuário"}
        message={
          target
            ? `Deseja ${target.active ? "inativar" : "ativar"} o usuário "${target.name}"?`
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
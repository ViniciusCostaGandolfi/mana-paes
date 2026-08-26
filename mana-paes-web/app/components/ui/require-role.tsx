import type { ReactNode } from "react";
import type { UserRole } from "~/types/api";
import { useAuth } from "~/hooks/use-auth";

interface RequireRoleProps {
  roles: UserRole[];
  children: ReactNode;
}

export function RequireRole({ roles, children }: RequireRoleProps) {
  const { user } = useAuth();

  if (!user || !roles.includes(user.role)) {
    return (
      <div role="alert" className="alert alert-warning sm:alert-horizontal">
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
            d="M12 9v3.75m9.303 3.376c-.866 1.5.217 3.374 1.948 3.374H7.052c-1.73 0-2.813-1.874-1.948-3.374L10.052 3.378c.866-1.5 3.032-1.5 3.898 0l8.353 12.748ZM12 15.75h.007v.008H12v-.008Z"
          />
        </svg>
        <span className="flex-1">
          Acesso negado. Você não tem permissão para visualizar este conteúdo.
        </span>
      </div>
    );
  }

  return <>{children}</>;
}

export default RequireRole;
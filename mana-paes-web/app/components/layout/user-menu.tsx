import { useNavigate } from "react-router";
import { useAuth } from "~/hooks/use-auth";
import { USER_ROLE_LABEL } from "~/lib/utils";

function getInitials(name: string | null | undefined): string {
  if (!name) return "?";
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0]?.toUpperCase() ?? "")
    .join("");
}

export function UserMenu() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  if (!user) return null;

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  return (
    <details className="dropdown dropdown-end">
      <summary
        className="btn btn-ghost gap-2 px-2"
        aria-label={`Menu do usuário: ${user.name}`}
      >
        <div className="avatar avatar-placeholder">
          <div className="bg-neutral text-neutral-content w-9 rounded-full">
            <span className="text-sm font-semibold">
              {getInitials(user.name)}
            </span>
          </div>
        </div>
        <span className="hidden text-sm font-medium sm:inline">
          {user.name}
        </span>
      </summary>
      <ul className="dropdown-content menu z-50 w-56 rounded-box border border-base-200 bg-base-100 p-2 shadow-lg">
        <li>
          <div className="flex flex-col items-start gap-0.5">
            <span className="truncate text-sm font-medium">{user.name}</span>
            <span className="badge badge-soft badge-primary badge-sm">
              {USER_ROLE_LABEL[user.role]}
            </span>
          </div>
        </li>
        <li className="mt-1 border-t border-base-200">
          <button type="button" className="text-error" onClick={handleLogout}>
            Sair
          </button>
        </li>
      </ul>
    </details>
  );
}

export default UserMenu;
import { Navigate, Outlet } from "react-router";
import { useAuth } from "~/hooks/use-auth";

export default function AdminLayout() {
  const { isAdmin } = useAuth();

  if (!isAdmin) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
import { Link } from "react-router";
import { useQuery } from "@tanstack/react-query";
import { useAuth } from "~/hooks/use-auth";
import { getWhatsappStatus } from "~/services/whatsapp.service";

/**
 * Banner de onboarding exibido apenas ao SYS_ADMIN enquanto o WhatsApp
 * global do tenant raiz não estiver conectado. Some quando `state == OPEN`.
 */
export function WhatsappOnboardingBanner() {
  const { isSysAdmin } = useAuth();

  const statusQuery = useQuery({
    queryKey: ["whatsapp-status", "onboarding"],
    queryFn: getWhatsappStatus,
    enabled: isSysAdmin,
    refetchInterval: 15_000,
  });

  if (!isSysAdmin) return null;
  if (statusQuery.isLoading || statusQuery.isError) return null;
  if (statusQuery.data?.state === "OPEN") return null;

  return (
    <div role="alert" className="alert alert-info sm:alert-horizontal">
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
          d="M8.625 12a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H8.25m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0H12m4.125 0a.375.375 0 1 1-.75 0 .375.375 0 0 1 .75 0Zm0 0h-.375M21 12c0 4.556-4.03 8.25-9 8.25a9.764 9.764 0 0 1-2.555-.337A5.972 5.972 0 0 1 5.41 20.97a5.969 5.969 0 0 1-.474-.065 4.48 4.48 0 0 0 .978-2.025c.09-.457-.133-.901-.467-1.226C3.93 16.178 3 14.189 3 12c0-4.556 4.03-8.25 9-8.25s9 3.694 9 8.25Z"
        />
      </svg>
      <div className="flex-1">
        <p className="font-medium">
          Conecte seu WhatsApp para enviar notificações
        </p>
        <p className="text-sm">
          Com o WhatsApp conectado, o sistema consegue enviar lembretes de
          pedido e avisos aos vendedores.
        </p>
      </div>
      <Link to="/settings/whatsapp" className="btn btn-info btn-sm">
        Conectar agora
      </Link>
    </div>
  );
}

export default WhatsappOnboardingBanner;
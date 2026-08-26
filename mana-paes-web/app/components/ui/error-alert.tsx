interface ErrorAlertProps {
  error: unknown;
  onRetry?: () => void;
}

function getErrorMessage(error: unknown): string {
  if (typeof error === "string" && error.trim().length > 0) {
    return error;
  }
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  if (
    error &&
    typeof error === "object" &&
    "message" in error &&
    typeof error.message === "string"
  ) {
    return error.message;
  }
  return "Ocorreu um erro inesperado. Tente novamente.";
}

export function ErrorAlert({ error, onRetry }: ErrorAlertProps) {
  return (
    <div role="alert" className="alert alert-error sm:alert-horizontal">
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
          d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
        />
      </svg>
      <span className="flex-1">{getErrorMessage(error)}</span>
      {onRetry ? (
        <button type="button" className="btn btn-sm btn-error" onClick={onRetry}>
          Tentar novamente
        </button>
      ) : null}
    </div>
  );
}

export default ErrorAlert;
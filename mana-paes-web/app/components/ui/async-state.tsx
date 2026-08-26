import type { ReactNode } from "react";
import { EmptyState } from "./empty-state";
import { ErrorAlert } from "./error-alert";
import { Spinner } from "./spinner";

interface AsyncStateProps {
  isLoading: boolean;
  isError?: boolean;
  error?: unknown;
  isEmpty?: boolean;
  emptyMessage?: string;
  emptyHint?: string;
  onRetry?: () => void;
  children: ReactNode;
}

export function AsyncState({
  isLoading,
  isError = false,
  error,
  isEmpty = false,
  emptyMessage = "Nenhum registro encontrado.",
  emptyHint,
  onRetry,
  children,
}: AsyncStateProps) {
  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <Spinner size="lg" />
      </div>
    );
  }

  if (isError) {
    return <ErrorAlert error={error} onRetry={onRetry} />;
  }

  if (isEmpty) {
    return <EmptyState message={emptyMessage} hint={emptyHint} />;
  }

  return <>{children}</>;
}

export default AsyncState;
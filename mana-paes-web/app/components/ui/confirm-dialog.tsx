import {
  useEffect,
  useRef,
  type MouseEvent,
  type SyntheticEvent,
} from "react";
import { cn } from "~/lib/utils";

interface ConfirmDialogProps {
  open: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
  danger?: boolean;
}

export function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "Confirmar",
  cancelLabel = "Cancelar",
  onConfirm,
  onCancel,
  isLoading = false,
  danger = false,
}: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  // Fecha via tecla ESC (evento "cancel" do <dialog>); bloqueia enquanto carrega.
  const handleDialogCancel = (event: SyntheticEvent<HTMLDialogElement>) => {
    if (isLoading) {
      event.preventDefault();
      return;
    }
    onCancel();
  };

  // Fecha ao clicar fora (backdrop); bloqueia o submit do form enquanto carrega.
  const handleBackdropClick = (event: MouseEvent<HTMLButtonElement>) => {
    if (isLoading) {
      event.preventDefault();
      return;
    }
    onCancel();
  };

  return (
    <dialog
      ref={dialogRef}
      className="modal"
      onCancel={handleDialogCancel}
    >
      <div className="modal-box">
        <h3 className="text-lg font-bold">{title}</h3>
        <p className="py-4 text-base-content/80">{message}</p>
        <div className="modal-action">
          <button
            type="button"
            className="btn"
            onClick={onCancel}
            disabled={isLoading}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            className={cn("btn", danger ? "btn-error" : "btn-primary")}
            onClick={onConfirm}
            disabled={isLoading}
          >
            {isLoading ? (
              <span
                className="loading loading-spinner loading-xs"
                aria-hidden="true"
              />
            ) : null}
            {confirmLabel}
          </button>
        </div>
      </div>
      <form method="dialog" className="modal-backdrop">
        <button type="submit" aria-label="Fechar" onClick={handleBackdropClick}>
          fechar
        </button>
      </form>
    </dialog>
  );
}

export default ConfirmDialog;
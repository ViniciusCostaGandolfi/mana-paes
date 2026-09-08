import { useEffect, useRef, useState } from "react";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { AsyncState } from "~/components/ui/async-state";
import { ConfirmDialog } from "~/components/ui/confirm-dialog";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";
import { PageHeader } from "~/components/ui/page-header";
import { Pagination } from "~/components/ui/pagination";
import { getApiErrorMessage } from "~/lib/api";
import { queryClient } from "~/lib/query";
import { cn, formatDate } from "~/lib/utils";
import {
  createReminder,
  deleteReminder,
  listReminders,
  testReminder,
  updateReminder,
} from "~/services/reminder.service";
import type { ScheduledReminderRequest, ScheduledReminderResponse } from "~/types/api";

const PAGE_SIZE = 20;

const reminderSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, "Informe o nome do lembrete (mínimo 2 caracteres).")
    .max(100, "O nome deve ter no máximo 100 caracteres."),
  message: z
    .string()
    .trim()
    .min(1, "Informe a mensagem do lembrete.")
    .max(500, "A mensagem deve ter no máximo 500 caracteres."),
  reminderTime: z
    .string()
    .regex(/^\d{2}:\d{2}$/, "Informe o horário no formato HH:mm."),
  deliveryDayOffset: z.coerce
    .number({ message: "Informe o dia antes da entrega." })
    .int("Informe um número inteiro.")
    .min(0, "O dia antes da entrega não pode ser negativo.")
    .max(30, "O máximo é 30 dias antes."),
  whatsappEnabled: z.boolean(),
  emailEnabled: z.boolean(),
  enabled: z.boolean(),
});

type ReminderFormInput = z.input<typeof reminderSchema>;
type ReminderFormValues = z.output<typeof reminderSchema>;

interface ReminderFormDialogProps {
  open: boolean;
  reminder: ScheduledReminderResponse | null;
  isPending: boolean;
  error: string | null;
  onClose: () => void;
  onSubmit: (values: ReminderFormValues) => void;
}

function ReminderFormDialog({
  open,
  reminder,
  isPending,
  error,
  onClose,
  onSubmit,
}: ReminderFormDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ReminderFormInput, unknown, ReminderFormValues>({
    resolver: zodResolver(reminderSchema),
    defaultValues: {
      name: "",
      message: "",
      reminderTime: "08:00",
      deliveryDayOffset: 1,
      whatsappEnabled: true,
      emailEnabled: false,
      enabled: true,
    },
  });

  useEffect(() => {
    if (!open) return;
    reset({
      name: reminder?.name ?? "",
      message: reminder?.message ?? "",
      reminderTime: reminder?.reminderTime ?? "08:00",
      deliveryDayOffset: reminder?.deliveryDayOffset ?? 1,
      whatsappEnabled: reminder?.whatsappEnabled ?? true,
      emailEnabled: reminder?.emailEnabled ?? false,
      enabled: reminder?.enabled ?? true,
    });
  }, [open, reminder, reset]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={dialogRef}
      className="modal"
      onCancel={(event) => {
        if (isPending) {
          event.preventDefault();
          return;
        }
        onClose();
      }}
    >
      <div className="modal-box max-w-lg">
        <h3 className="text-lg font-bold">
          {reminder ? "Editar lembrete" : "Novo lembrete"}
        </h3>

        <form
          className="mt-4 space-y-4"
          onSubmit={handleSubmit(onSubmit)}
          noValidate
        >
          {error ? <ErrorAlert error={error} /> : null}

          <FormField label="Nome" required error={errors.name?.message}>
            <input
              type="text"
              className="input w-full"
              placeholder="Ex.: Lembrete de pedidos"
              {...register("name")}
            />
          </FormField>

          <FormField label="Mensagem" required error={errors.message?.message}>
            <textarea
              className="textarea textarea-bordered w-full"
              rows={3}
              placeholder="Mensagem enviada aos vendedores no horário agendado."
              {...register("message")}
            />
          </FormField>

          <div className="grid gap-4 sm:grid-cols-2">
            <FormField
              label="Horário"
              required
              error={errors.reminderTime?.message}
              hint="Horário em que o lembrete é enviado (HH:mm)."
            >
              <input
                type="time"
                className="input w-full"
                {...register("reminderTime")}
              />
            </FormField>

            <FormField
              label="Dias antes da entrega"
              required
              error={errors.deliveryDayOffset?.message}
              hint="Ex.: 1 envia o lembrete no dia anterior à entrega."
            >
              <input
                type="number"
                min={0}
                max={30}
                className="input w-full"
                {...register("deliveryDayOffset")}
              />
            </FormField>
          </div>

          <label className="flex cursor-pointer items-center justify-between gap-3 rounded-box border border-base-300 px-4 py-3">
            <span className="text-sm font-medium text-base-content">
              Enviar por WhatsApp
            </span>
            <input
              type="checkbox"
              className="toggle toggle-primary"
              {...register("whatsappEnabled")}
            />
          </label>

          <label className="flex cursor-pointer items-center justify-between gap-3 rounded-box border border-base-300 px-4 py-3">
            <span className="text-sm font-medium text-base-content">
              Enviar por e-mail
            </span>
            <input
              type="checkbox"
              className="toggle toggle-primary"
              {...register("emailEnabled")}
            />
          </label>

          <label className="flex cursor-pointer items-center justify-between gap-3 rounded-box border border-base-300 px-4 py-3">
            <span className="text-sm font-medium text-base-content">
              Lembrete ativo
            </span>
            <input
              type="checkbox"
              className="toggle toggle-success"
              {...register("enabled")}
            />
          </label>

          <div className="modal-action">
            <button
              type="button"
              className="btn"
              onClick={onClose}
              disabled={isPending}
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="btn btn-primary"
              disabled={isPending}
            >
              {isPending ? (
                <span
                  className="loading loading-spinner loading-xs"
                  aria-hidden="true"
                />
              ) : null}
              {reminder ? "Salvar alterações" : "Criar lembrete"}
            </button>
          </div>
        </form>
      </div>

      <form method="dialog" className="modal-backdrop">
        <button
          type="submit"
          aria-label="Fechar"
          onClick={(event) => {
            if (isPending) event.preventDefault();
            else onClose();
          }}
        >
          fechar
        </button>
      </form>
    </dialog>
  );
}

function formatLastSent(value: string | null): string {
  if (!value) return "Nunca";
  return formatDate(value);
}

export default function RemindersSettingsPage() {
  // `page` segue a convenção 0-based da API (PageResponse.number do backend).
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ScheduledReminderResponse | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<ScheduledReminderResponse | null>(null);
  const [testTarget, setTestTarget] = useState<ScheduledReminderResponse | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const remindersQuery = useQuery({
    queryKey: ["reminders", page],
    queryFn: () => listReminders({ page, size: PAGE_SIZE }),
  });

  const data = remindersQuery.data;
  const reminders = data?.content ?? [];

  const saveMutation = useMutation({
    mutationFn: (values: ReminderFormValues) => {
      const payload: ScheduledReminderRequest = {
        name: values.name,
        message: values.message,
        reminderTime: values.reminderTime,
        deliveryDayOffset: values.deliveryDayOffset,
        whatsappEnabled: values.whatsappEnabled,
        emailEnabled: values.emailEnabled,
        enabled: values.enabled,
      };
      return editing ? updateReminder(editing.id, payload) : createReminder(payload);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reminders"] });
      setFormError(null);
      setFormOpen(false);
      setEditing(null);
      setSuccessMessage(
        editing ? "Lembrete atualizado com sucesso." : "Lembrete criado com sucesso.",
      );
    },
    onError: (err) => setFormError(getApiErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteReminder(deleteTarget!.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["reminders"] });
      setDeleteTarget(null);
      setActionError(null);
      setSuccessMessage("Lembrete excluído com sucesso.");
    },
    onError: (err) => {
      setActionError(getApiErrorMessage(err));
      setDeleteTarget(null);
    },
  });

  const testMutation = useMutation({
    mutationFn: () => testReminder(testTarget!.id),
    onSuccess: (result) => {
      queryClient.invalidateQueries({ queryKey: ["reminders"] });
      setTestTarget(null);
      setActionError(null);
      setSuccessMessage(
        `Lembrete enviado a ${result.recipients} vendedor(es) por WhatsApp (${result.whatsappSent} enviado(s)).`,
      );
    },
    onError: (err) => {
      setActionError(getApiErrorMessage(err));
      setTestTarget(null);
    },
  });

  const handleOpenCreate = () => {
    setFormError(null);
    setEditing(null);
    setFormOpen(true);
  };

  const handleOpenEdit = (reminder: ScheduledReminderResponse) => {
    setFormError(null);
    setEditing(reminder);
    setFormOpen(true);
  };

  const handleFormSubmit = (values: ReminderFormValues) => {
    setSuccessMessage(null);
    saveMutation.mutate(values);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Lembretes"
        subtitle="Mensagens automáticas enviadas aos vendedores ativos com o link de pedido."
        actions={
          <button type="button" className="btn btn-primary" onClick={handleOpenCreate}>
            Novo lembrete
          </button>
        }
      />

      {actionError ? (
        <ErrorAlert error={actionError} onRetry={() => setActionError(null)} />
      ) : null}
      {successMessage ? (
        <div role="alert" className="alert alert-success sm:alert-horizontal">
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
              d="M9 12.75 11.25 15 15 9.75M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z"
            />
          </svg>
          <span className="flex-1">{successMessage}</span>
        </div>
      ) : null}

      <AsyncState
        isLoading={remindersQuery.isLoading}
        isError={remindersQuery.isError}
        error={getApiErrorMessage(remindersQuery.error)}
        isEmpty={reminders.length === 0}
        emptyMessage="Nenhum lembrete configurado."
        emptyHint='Crie um lembrete pelo botão "Novo lembrete".'
        onRetry={() => remindersQuery.refetch()}
      >
        <div className="overflow-x-auto rounded-box border border-base-300 bg-base-100">
          <table className="table">
            <thead>
              <tr>
                <th>Nome</th>
                <th>Horário</th>
                <th>Dias antes</th>
                <th>Canais</th>
                <th>Status</th>
                <th>Último envio</th>
                <th className="text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {reminders.map((reminder) => (
                <tr
                  key={reminder.id}
                  className={cn(!reminder.enabled && "opacity-60")}
                >
                  <td className="font-medium">{reminder.name}</td>
                  <td>{reminder.reminderTime}</td>
                  <td>
                    {reminder.deliveryDayOffset === 0
                      ? "No dia"
                      : `${reminder.deliveryDayOffset} dia(s)`}
                  </td>
                  <td>
                    <div className="flex flex-wrap gap-1.5">
                      {reminder.whatsappEnabled ? (
                        <span className="badge badge-success badge-outline">
                          WhatsApp
                        </span>
                      ) : null}
                      {reminder.emailEnabled ? (
                        <span className="badge badge-info badge-outline">
                          E-mail
                        </span>
                      ) : null}
                      {!reminder.whatsappEnabled && !reminder.emailEnabled ? (
                        <span className="text-base-content/40">—</span>
                      ) : null}
                    </div>
                  </td>
                  <td>
                    <span
                      className={cn(
                        "badge",
                        reminder.enabled ? "badge-success" : "badge-ghost",
                      )}
                    >
                      {reminder.enabled ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td className="whitespace-nowrap">
                    {formatLastSent(reminder.lastSentDate)}
                  </td>
                  <td>
                    <div className="flex justify-end gap-2">
                      <button
                        type="button"
                        className="btn btn-sm btn-outline"
                        disabled={testMutation.isPending}
                        onClick={() => {
                          setActionError(null);
                          setTestTarget(reminder);
                        }}
                      >
                        Enviar agora
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm btn-outline"
                        onClick={() => handleOpenEdit(reminder)}
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm btn-outline btn-error"
                        onClick={() => {
                          setActionError(null);
                          setDeleteTarget(reminder);
                        }}
                      >
                        Excluir
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {data && data.page.totalElements > 0 ? (
          <Pagination
            page={page + 1}
            totalPages={data.page.totalPages}
            totalElements={data.page.totalElements}
            size={data.page.size}
            onChange={(nextPage) => setPage(nextPage - 1)}
          />
        ) : null}
      </AsyncState>

      <ReminderFormDialog
        open={formOpen}
        reminder={editing}
        isPending={saveMutation.isPending}
        error={formError}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
        onSubmit={handleFormSubmit}
      />

      <ConfirmDialog
        open={deleteTarget !== null}
        title="Excluir lembrete"
        message={
          deleteTarget
            ? `Deseja realmente excluir o lembrete "${deleteTarget.name}"? Esta ação não pode ser desfeita.`
            : ""
        }
        confirmLabel="Excluir"
        cancelLabel="Cancelar"
        danger
        isLoading={deleteMutation.isPending}
        onConfirm={() => deleteMutation.mutate()}
        onCancel={() => setDeleteTarget(null)}
      />

      <ConfirmDialog
        open={testTarget !== null}
        title="Enviar lembrete agora"
        message={
          testTarget
            ? `O lembrete "${testTarget.name}" será enviado imediatamente a todos os vendedores ativos. Deseja continuar?`
            : ""
        }
        confirmLabel="Enviar agora"
        cancelLabel="Cancelar"
        isLoading={testMutation.isPending}
        onConfirm={() => testMutation.mutate()}
        onCancel={() => setTestTarget(null)}
      />
    </div>
  );
}
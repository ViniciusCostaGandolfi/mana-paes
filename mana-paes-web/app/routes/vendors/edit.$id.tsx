import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ConfirmDialog } from "~/components/ui/confirm-dialog";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";
import { PageHeader } from "~/components/ui/page-header";
import { Spinner } from "~/components/ui/spinner";
import { getApiErrorMessage } from "~/lib/api";
import { queryClient } from "~/lib/query";
import { cn, formatPhoneBR, normalizePhoneBR } from "~/lib/utils";
import { getVendor, setVendorActive, updateVendor } from "~/services/vendor.service";

const vendorSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, "Informe o nome do vendedor (mínimo 2 caracteres).")
    .max(150, "O nome deve ter no máximo 150 caracteres."),
  phone: z
    .string()
    .trim()
    .min(8, "Informe o telefone (mínimo 8 caracteres).")
    .max(20, "O telefone deve ter no máximo 20 caracteres."),
  whatsappNumber: z
    .string()
    .trim()
    .max(20, "O WhatsApp deve ter no máximo 20 caracteres.")
    .optional(),
});

type VendorFormValues = z.infer<typeof vendorSchema>;

export default function EditVendorPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [toggleError, setToggleError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { data: vendor, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["vendor", id],
    queryFn: () => getVendor(id!),
    enabled: !!id,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<VendorFormValues>({
    resolver: zodResolver(vendorSchema),
    defaultValues: { name: "", phone: "", whatsappNumber: "" },
  });

  useEffect(() => {
    if (vendor) {
      reset({
        name: vendor.name,
        phone: formatPhoneBR(vendor.phone),
        whatsappNumber: formatPhoneBR(vendor.whatsappNumber),
      });
    }
  }, [vendor, reset]);

  const updateMutation = useMutation({
    mutationFn: (values: VendorFormValues) =>
      updateVendor(id!, {
        name: values.name,
        phone: normalizePhoneBR(values.phone),
        whatsappNumber: values.whatsappNumber?.trim()
          ? normalizePhoneBR(values.whatsappNumber)
          : null,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["vendors"] });
      navigate("/vendors");
    },
    onError: (err) => setSubmitError(getApiErrorMessage(err)),
  });

  const toggleMutation = useMutation({
    mutationFn: (active: boolean) => setVendorActive(id!, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["vendor", id] });
      queryClient.invalidateQueries({ queryKey: ["vendors"] });
      setConfirmOpen(false);
      setToggleError(null);
    },
    onError: (err) => {
      setToggleError(getApiErrorMessage(err));
      setConfirmOpen(false);
    },
  });

  const onSubmit = (values: VendorFormValues) => {
    setSubmitError(null);
    updateMutation.mutate(values);
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader title="Editar vendedor" />
        <div className="flex items-center justify-center py-16">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="space-y-6">
        <PageHeader title="Editar vendedor" />
        <ErrorAlert error={error} onRetry={() => refetch()} />
      </div>
    );
  }

  if (!vendor) return null;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Editar vendedor"
        actions={
          <div className="flex items-center gap-2">
            <span
              className={cn(
                "badge",
                vendor.active ? "badge-success badge-outline" : "badge-ghost",
              )}
            >
              {vendor.active ? "Ativo" : "Inativo"}
            </span>
            <button
              type="button"
              className="btn btn-sm"
              onClick={() => {
                setToggleError(null);
                setConfirmOpen(true);
              }}
            >
              {vendor.active ? "Inativar" : "Ativar"}
            </button>
          </div>
        }
      />

      <div className="max-w-2xl">
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body">
            {submitError ? <ErrorAlert error={submitError} /> : null}
            {toggleError ? (
              <ErrorAlert error={toggleError} onRetry={() => setToggleError(null)} />
            ) : null}

            <form
              onSubmit={handleSubmit(onSubmit)}
              className="space-y-4"
              noValidate
            >
              <div className="grid gap-4 sm:grid-cols-2">
                <FormField label="Nome" required error={errors.name?.message}>
                  <input
                    type="text"
                    className="input w-full"
                    placeholder="Ex.: João da Esquina"
                    {...register("name")}
                  />
                </FormField>

                <FormField
                  label="Telefone"
                  required
                  error={errors.phone?.message}
                  hint="Identifica o vendedor no link de pedido. O DDI 55 é adicionado automaticamente."
                >
                  <input
                    type="text"
                    className="input w-full"
                    placeholder="Ex.: 11999998888"
                    {...register("phone")}
                  />
                </FormField>

                <FormField
                  label="WhatsApp"
                  error={errors.whatsappNumber?.message}
                  hint="Opcional. Usado nas notificações quando informado. O DDI 55 é adicionado automaticamente."
                >
                  <input
                    type="text"
                    className="input w-full"
                    placeholder="Ex.: 11999998888"
                    {...register("whatsappNumber")}
                  />
                </FormField>
              </div>

              <div className="card-actions justify-end pt-2">
                <Link to="/vendors" className="btn">
                  Cancelar
                </Link>
                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={updateMutation.isPending}
                >
                  {updateMutation.isPending ? (
                    <span
                      className="loading loading-spinner loading-xs"
                      aria-hidden="true"
                    />
                  ) : null}
                  Salvar
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={confirmOpen}
        title={vendor.active ? "Inativar vendedor" : "Ativar vendedor"}
        message={`Deseja ${vendor.active ? "inativar" : "ativar"} o vendedor "${vendor.name}"?`}
        confirmLabel={vendor.active ? "Inativar" : "Ativar"}
        onConfirm={() => toggleMutation.mutate(!vendor.active)}
        onCancel={() => setConfirmOpen(false)}
        isLoading={toggleMutation.isPending}
        danger={vendor.active}
      />
    </div>
  );
}
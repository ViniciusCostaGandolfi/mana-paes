import { useState } from "react";
import { Link, useNavigate } from "react-router";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";
import { PageHeader } from "~/components/ui/page-header";
import { getApiErrorMessage } from "~/lib/api";
import { queryClient } from "~/lib/query";
import { normalizePhoneBR } from "~/lib/utils";
import { createVendor } from "~/services/vendor.service";

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

export default function NewVendorPage() {
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<VendorFormValues>({
    resolver: zodResolver(vendorSchema),
    defaultValues: { name: "", phone: "", whatsappNumber: "" },
  });

  const createMutation = useMutation({
    mutationFn: createVendor,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["vendors"] });
      navigate("/vendors");
    },
    onError: (error) => setSubmitError(getApiErrorMessage(error)),
  });

  const onSubmit = (values: VendorFormValues) => {
    setSubmitError(null);
    createMutation.mutate({
      name: values.name,
      phone: normalizePhoneBR(values.phone),
      whatsappNumber: values.whatsappNumber?.trim()
        ? normalizePhoneBR(values.whatsappNumber)
        : null,
    });
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Novo vendedor"
        subtitle="O telefone é o identificador do link público de pedido."
      />

      <div className="max-w-2xl">
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body">
            {submitError ? <ErrorAlert error={submitError} /> : null}

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
                  disabled={createMutation.isPending}
                >
                  {createMutation.isPending ? (
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
    </div>
  );
}
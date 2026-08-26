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
import { USER_ROLE_LABEL } from "~/lib/utils";
import { createUser } from "~/services/user.service";
import type { UserRole } from "~/types/api";

const userSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, "Informe o nome do usuário (mínimo 2 caracteres).")
    .max(150, "O nome deve ter no máximo 150 caracteres."),
  email: z
    .string()
    .trim()
    .email("Informe um e-mail válido.")
    .max(254, "O e-mail deve ter no máximo 254 caracteres."),
  phone: z
    .string()
    .trim()
    .max(30, "O telefone deve ter no máximo 30 caracteres.")
    .optional(),
  whatsappNumber: z
    .string()
    .trim()
    .max(30, "O WhatsApp deve ter no máximo 30 caracteres.")
    .optional(),
  role: z.enum(["ROLE_ADMIN", "ROLE_REQUESTER", "ROLE_PRODUCTION"]),
});

type UserFormInput = z.input<typeof userSchema>;
type UserFormValues = z.output<typeof userSchema>;

export default function NewUserPage() {
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<UserFormInput, unknown, UserFormValues>({
    resolver: zodResolver(userSchema),
    defaultValues: {
      name: "",
      email: "",
      phone: "",
      whatsappNumber: "",
      role: "ROLE_REQUESTER",
    },
  });

  const createMutation = useMutation({
    mutationFn: createUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["users"] });
      navigate("/users");
    },
    onError: (error) => setSubmitError(getApiErrorMessage(error)),
  });

  const onSubmit = (values: UserFormValues) => {
    setSubmitError(null);
    createMutation.mutate({
      name: values.name,
      email: values.email,
      phone: values.phone || undefined,
      whatsappNumber: values.whatsappNumber || undefined,
      role: values.role,
    });
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Novo usuário"
        subtitle="A senha inicial é definida via recuperação de senha por e-mail."
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
                    placeholder="Ex.: Maria Silva"
                    {...register("name")}
                  />
                </FormField>

                <FormField
                  label="E-mail"
                  required
                  error={errors.email?.message}
                >
                  <input
                    type="email"
                    className="input w-full"
                    placeholder="exemplo@email.com"
                    {...register("email")}
                  />
                </FormField>

                <FormField
                  label="Telefone"
                  error={errors.phone?.message}
                  hint="Opcional."
                >
                  <input
                    type="tel"
                    className="input w-full"
                    placeholder="(00) 0000-0000"
                    {...register("phone")}
                  />
                </FormField>

                <FormField
                  label="WhatsApp"
                  error={errors.whatsappNumber?.message}
                  hint="Opcional."
                >
                  <input
                    type="tel"
                    className="input w-full"
                    placeholder="(00) 00000-0000"
                    {...register("whatsappNumber")}
                  />
                </FormField>

                <FormField
                  label="Perfil"
                  required
                  error={errors.role?.message}
                >
                  <select className="select w-full" {...register("role")}>
                    {(Object.keys(USER_ROLE_LABEL) as UserRole[]).map(
                      (value) => (
                        <option key={value} value={value}>
                          {USER_ROLE_LABEL[value]}
                        </option>
                      ),
                    )}
                  </select>
                </FormField>
              </div>

              <div className="card-actions justify-end pt-2">
                <Link to="/users" className="btn">
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
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
import { createUser } from "~/services/user.service";
import type { UserRequest } from "~/types/api";
import { useAuth } from "~/hooks/use-auth";

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
  password: z.string().min(6, "A senha deve ter pelo menos 6 caracteres."),
});

type UserFormValues = z.infer<typeof userSchema>;

export default function NewUserPage() {
  const { isSysAdmin } = useAuth();
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<UserFormValues>({
    resolver: zodResolver(userSchema),
    defaultValues: { name: "", email: "", password: "" },
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
    const payload: UserRequest = {
      name: values.name,
      email: values.email,
      role: "ROLE_ADMIN",
      password: values.password,
    };
    createMutation.mutate(payload);
  };

  if (!isSysAdmin) {
    return (
      <div className="space-y-6">
        <PageHeader title="Novo usuário" />
        <ErrorAlert error="Apenas o administrador do sistema pode criar usuários." />
        <Link to="/users" className="btn">
          Voltar
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Novo usuário"
        subtitle="Crie um administrador para a padaria. O acesso é criado com a senha informada."
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
                    placeholder="Ex.: Breno Silva"
                    {...register("name")}
                  />
                </FormField>

                <FormField label="E-mail" required error={errors.email?.message}>
                  <input
                    type="email"
                    className="input w-full"
                    placeholder="exemplo@email.com"
                    {...register("email")}
                  />
                </FormField>

                <FormField
                  label="Senha inicial"
                  required
                  error={errors.password?.message}
                  hint="O administrador usa esta senha para entrar no sistema."
                >
                  <input
                    type="password"
                    autoComplete="new-password"
                    className="input w-full"
                    placeholder="Mínimo de 6 caracteres"
                    {...register("password")}
                  />
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
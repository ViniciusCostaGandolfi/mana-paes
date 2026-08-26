import { Link } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { z } from "zod";
import { forgotPassword } from "~/services/auth.service";
import { getApiErrorMessage } from "~/lib/api";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";

const forgotPasswordSchema = z.object({
  email: z.string().email("Informe um e-mail válido."),
});

type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;

export default function ForgotPasswordPage() {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ForgotPasswordFormValues>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: "" },
  });

  const mutation = useMutation({
    mutationFn: async (values: ForgotPasswordFormValues) => {
      try {
        return await forgotPassword(values.email);
      } catch (error) {
        throw new Error(getApiErrorMessage(error));
      }
    },
  });

  return (
    <main className="flex min-h-screen items-center justify-center bg-base-200 p-4">
      <div className="card w-full max-w-sm bg-base-100 shadow-xl">
        <div className="card-body">
          <div className="flex flex-col items-center gap-2 text-center">
            <img
              src="/logo.svg"
              alt="Logo Mana Paes"
              className="h-16 w-16 rounded-box"
            />
            <h1 className="card-title text-2xl">Mana Paes</h1>
            <p className="text-sm text-base-content/70">Recuperar senha</p>
            <p className="text-sm text-base-content/60">
              Enviaremos um link de redefinição para o seu e-mail.
            </p>
          </div>

          {mutation.isSuccess && mutation.data ? (
            <div className="mt-4 flex flex-col gap-4">
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
                <span className="flex-1">{mutation.data.message}</span>
              </div>
              <Link to="/login" className="link link-hover link-primary text-center">
                Voltar para o login
              </Link>
            </div>
          ) : (
            <form
              className="mt-4 flex flex-col gap-4"
              onSubmit={handleSubmit((values) => mutation.mutate(values))}
              noValidate
            >
              {mutation.isError ? <ErrorAlert error={mutation.error} /> : null}

              <FormField label="E-mail" required error={errors.email?.message}>
                <input
                  type="email"
                  autoComplete="email"
                  placeholder="voce@exemplo.com"
                  className="input input-bordered w-full"
                  {...register("email")}
                />
              </FormField>

              <button
                type="submit"
                className="btn btn-primary w-full"
                disabled={mutation.isPending}
              >
                {mutation.isPending ? (
                  <span className="loading loading-spinner loading-sm" />
                ) : null}
                {mutation.isPending ? "Enviando..." : "Enviar link de recuperação"}
              </button>
            </form>
          )}

          <div className="mt-4 text-center text-sm">
            <Link to="/login" className="link link-hover link-primary">
              Voltar para o login
            </Link>
          </div>
        </div>
      </div>
    </main>
  );
}
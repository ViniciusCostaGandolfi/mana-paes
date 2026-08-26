import { Link, Navigate, useNavigate } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { z } from "zod";
import { useAuth } from "~/hooks/use-auth";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";

const loginSchema = z.object({
  email: z.string().email("Informe um e-mail válido."),
  password: z.string().min(1, "Informe sua senha."),
});

type LoginFormValues = z.infer<typeof loginSchema>;

export default function LoginPage() {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  });

  const mutation = useMutation({
    mutationFn: (values: LoginFormValues) => login(values.email, values.password),
    onSuccess: () => {
      navigate("/dashboard", { replace: true });
    },
  });

  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

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
            <p className="text-sm text-base-content/70">Acesse sua conta</p>
          </div>

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

            <FormField label="Senha" required error={errors.password?.message}>
              <input
                type="password"
                autoComplete="current-password"
                placeholder="Sua senha"
                className="input input-bordered w-full"
                {...register("password")}
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
              {mutation.isPending ? "Entrando..." : "Entrar"}
            </button>
          </form>

          <div className="mt-4 flex flex-col items-center gap-1 text-sm">
            <Link to="/forgot-password" className="link link-hover link-primary">
              Esqueci minha senha
            </Link>
            <span className="text-base-content/60">
              Não tem uma conta?{" "}
              <Link to="/register" className="link link-hover link-primary">
                Criar conta
              </Link>
            </span>
          </div>
        </div>
      </div>
    </main>
  );
}
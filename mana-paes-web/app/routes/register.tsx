import { Link, useNavigate } from "react-router";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { z } from "zod";
import { register as registerUser } from "~/services/auth.service";
import type { RegisterRequest } from "~/types/api";
import { getApiErrorMessage } from "~/lib/api";
import { setStoredUser, setTokens } from "~/lib/auth";
import { useAuth } from "~/hooks/use-auth";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";

const registerSchema = z
  .object({
    name: z.string().min(2, "Informe seu nome."),
    email: z.string().email("Informe um e-mail válido."),
    password: z.string().min(6, "A senha deve ter pelo menos 6 caracteres."),
    confirmPassword: z.string().min(6, "Confirme sua senha."),
    phone: z.string().optional(),
    whatsappNumber: z.string().optional(),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "As senhas não coincidem.",
    path: ["confirmPassword"],
  });

type RegisterFormValues = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const { setUser } = useAuth();
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      name: "",
      email: "",
      password: "",
      confirmPassword: "",
      phone: "",
      whatsappNumber: "",
    },
  });

  const mutation = useMutation({
    mutationFn: async (values: RegisterFormValues) => {
      const payload: RegisterRequest = {
        name: values.name,
        email: values.email,
        password: values.password,
        phone: values.phone?.trim() || null,
        whatsappNumber: values.whatsappNumber?.trim() || null,
      };

      try {
        const response = await registerUser(payload);
        setTokens(response.accessToken, response.refreshToken);
        setStoredUser(response.user);
        setUser(response.user);
      } catch (error) {
        throw new Error(getApiErrorMessage(error));
      }
    },
    onSuccess: () => {
      navigate("/dashboard", { replace: true });
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
            <p className="text-sm text-base-content/70">Criar conta</p>
          </div>

          <form
            className="mt-4 flex flex-col gap-4"
            onSubmit={handleSubmit((values) => mutation.mutate(values))}
            noValidate
          >
            {mutation.isError ? <ErrorAlert error={mutation.error} /> : null}

            <FormField label="Nome" required error={errors.name?.message}>
              <input
                type="text"
                autoComplete="name"
                placeholder="Seu nome completo"
                className="input input-bordered w-full"
                {...register("name")}
              />
            </FormField>

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
                autoComplete="new-password"
                placeholder="Mínimo de 6 caracteres"
                className="input input-bordered w-full"
                {...register("password")}
              />
            </FormField>

            <FormField
              label="Confirmar senha"
              required
              error={errors.confirmPassword?.message}
            >
              <input
                type="password"
                autoComplete="new-password"
                placeholder="Repita a senha"
                className="input input-bordered w-full"
                {...register("confirmPassword")}
              />
            </FormField>

            <FormField label="Telefone" hint="Opcional" error={errors.phone?.message}>
              <input
                type="tel"
                autoComplete="tel"
                placeholder="(00) 00000-0000"
                className="input input-bordered w-full"
                {...register("phone")}
              />
            </FormField>

            <FormField
              label="WhatsApp"
              hint="Opcional"
              error={errors.whatsappNumber?.message}
            >
              <input
                type="tel"
                autoComplete="tel"
                placeholder="(00) 00000-0000"
                className="input input-bordered w-full"
                {...register("whatsappNumber")}
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
              {mutation.isPending ? "Criando conta..." : "Criar conta"}
            </button>
          </form>

          <div className="mt-4 text-center text-sm text-base-content/60">
            Já tem uma conta?{" "}
            <Link to="/login" className="link link-hover link-primary">
              Entrar
            </Link>
          </div>
        </div>
      </div>
    </main>
  );
}
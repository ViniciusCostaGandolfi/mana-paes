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
import { OptionGroupsEditor, type ProductFormValues } from "~/components/ui/option-groups-editor";
import { getApiErrorMessage } from "~/lib/api";
import { queryClient } from "~/lib/query";
import { UNIT_MEASURE_LABEL, cn } from "~/lib/utils";
import {
  getProduct,
  setProductActive,
  updateProduct,
} from "~/services/product.service";
import type { ProductRequest, UnitMeasure } from "~/types/api";

const optionSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Informe o nome da opção.")
    .max(100, "O nome da opção deve ter no máximo 100 caracteres."),
  priceAdjustment: z
    .string()
    .refine(
      (value) => value.trim() !== "" && !Number.isNaN(Number(value)),
      "Informe o acréscimo.",
    )
    .refine((value) => Number(value) >= 0, "O acréscimo não pode ser negativo."),
});

const optionGroupSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, "Informe o nome do grupo.")
    .max(100, "O nome do grupo deve ter no máximo 100 caracteres."),
  options: z.array(optionSchema).min(1, "Adicione pelo menos uma opção."),
});

const productSchema = z.object({
  name: z
    .string()
    .trim()
    .min(2, "Informe o nome do produto (mínimo 2 caracteres).")
    .max(200, "O nome deve ter no máximo 200 caracteres."),
  description: z
    .string()
    .trim()
    .max(500, "A descrição deve ter no máximo 500 caracteres.")
    .optional(),
  unitPrice: z
    .string()
    .min(1, "Informe o preço do produto.")
    .refine((value) => !Number.isNaN(Number(value)), "Informe um preço válido.")
    .refine((value) => Number(value) > 0, "O preço deve ser maior que zero."),
  unitMeasure: z.enum(["UN", "KG"]),
  optionGroups: z.array(optionGroupSchema).optional(),
});

export default function EditProductPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [toggleError, setToggleError] = useState<string | null>(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const { data: product, isLoading, isError, error, refetch } = useQuery({
    queryKey: ["product", id],
    queryFn: () => getProduct(id!),
    enabled: !!id,
  });

  const {
    register,
    handleSubmit,
    reset,
    control,
    formState: { errors },
  } = useForm<ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: "",
      description: "",
      unitPrice: "",
      unitMeasure: "UN",
      optionGroups: [],
    },
  });

  useEffect(() => {
    if (product) {
      reset({
        name: product.name,
        description: product.description ?? "",
        unitPrice: String(product.unitPrice),
        unitMeasure: product.unitMeasure,
        optionGroups: (product.optionGroups ?? []).map((group) => ({
          name: group.name,
          options: group.options.map((option) => ({
            name: option.name,
            priceAdjustment: String(option.priceAdjustment),
          })),
        })),
      });
    }
  }, [product, reset]);

  const updateMutation = useMutation({
    mutationFn: (values: ProductFormValues) =>
      updateProduct(id!, {
        name: values.name,
        description: values.description || undefined,
        unitPrice: Number(values.unitPrice),
        unitMeasure: values.unitMeasure,
        optionGroups: (values.optionGroups ?? []).map((group) => ({
          name: group.name,
          options: group.options.map((option) => ({
            name: option.name,
            priceAdjustment: Number(option.priceAdjustment) || 0,
          })),
        })),
      } satisfies ProductRequest),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["products"] });
      navigate("/products");
    },
    onError: (err) => setSubmitError(getApiErrorMessage(err)),
  });

  const toggleMutation = useMutation({
    mutationFn: (active: boolean) => setProductActive(id!, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["product", id] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      setConfirmOpen(false);
      setToggleError(null);
    },
    onError: (err) => {
      setToggleError(getApiErrorMessage(err));
      setConfirmOpen(false);
    },
  });

  const onSubmit = (values: ProductFormValues) => {
    setSubmitError(null);
    updateMutation.mutate(values);
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader title="Editar produto" />
        <div className="flex items-center justify-center py-16">
          <Spinner size="lg" />
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="space-y-6">
        <PageHeader title="Editar produto" />
        <ErrorAlert error={error} onRetry={() => refetch()} />
      </div>
    );
  }

  if (!product) return null;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Editar produto"
        actions={
          <div className="flex items-center gap-2">
            <span
              className={cn(
                "badge",
                product.active ? "badge-success badge-outline" : "badge-ghost",
              )}
            >
              {product.active ? "Ativo" : "Inativo"}
            </span>
            <button
              type="button"
              className="btn btn-sm"
              onClick={() => {
                setToggleError(null);
                setConfirmOpen(true);
              }}
            >
              {product.active ? "Inativar" : "Ativar"}
            </button>
          </div>
        }
      />

      <div className="max-w-3xl">
        <div className="card border border-base-300 bg-base-100">
          <div className="card-body">
            {submitError ? <ErrorAlert error={submitError} /> : null}
            {toggleError ? (
              <ErrorAlert error={toggleError} onRetry={() => setToggleError(null)} />
            ) : null}

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
              <div className="grid gap-4 sm:grid-cols-2">
                <FormField label="Nome" required error={errors.name?.message}>
                  <input
                    type="text"
                    className="input w-full"
                    placeholder="Ex.: Pão francês"
                    {...register("name")}
                  />
                </FormField>

                <FormField
                  label="Descrição"
                  error={errors.description?.message}
                  hint="Opcional. Detalhes adicionais sobre o produto."
                >
                  <textarea
                    className="textarea w-full"
                    rows={1}
                    placeholder="Ex.: Pão de fermentação natural"
                    {...register("description")}
                  />
                </FormField>

                <FormField label="Preço unitário" required error={errors.unitPrice?.message}>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    className="input w-full"
                    placeholder="0,00"
                    {...register("unitPrice")}
                  />
                </FormField>

                <FormField label="Unidade de medida" required error={errors.unitMeasure?.message}>
                  <select className="select w-full" {...register("unitMeasure")}>
                    {(Object.keys(UNIT_MEASURE_LABEL) as UnitMeasure[]).map((value) => (
                      <option key={value} value={value}>
                        {UNIT_MEASURE_LABEL[value]}
                      </option>
                    ))}
                  </select>
                </FormField>
              </div>

              <div className="border-t border-base-200 pt-4">
                <OptionGroupsEditor control={control} register={register} errors={errors} />
              </div>

              <div className="card-actions justify-end pt-2">
                <Link to="/products" className="btn">
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
        title={product.active ? "Inativar produto" : "Ativar produto"}
        message={`Deseja ${product.active ? "inativar" : "ativar"} o produto "${product.name}"?`}
        confirmLabel={product.active ? "Inativar" : "Ativar"}
        onConfirm={() => toggleMutation.mutate(!product.active)}
        onCancel={() => setConfirmOpen(false)}
        isLoading={toggleMutation.isPending}
        danger={product.active}
      />
    </div>
  );
}
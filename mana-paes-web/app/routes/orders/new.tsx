import { useMemo } from "react";
import { Link, useNavigate } from "react-router";
import { useFieldArray, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { z } from "zod";
import { EmptyState } from "~/components/ui/empty-state";
import { ErrorAlert } from "~/components/ui/error-alert";
import { FormField } from "~/components/ui/form-field";
import { PageHeader } from "~/components/ui/page-header";
import { useAuth } from "~/hooks/use-auth";
import { getApiErrorMessage } from "~/lib/api";
import { formatCurrency, UNIT_MEASURE_LABEL } from "~/lib/utils";
import { createOrder } from "~/services/order.service";
import { listProducts } from "~/services/product.service";
import type { ProductResponse } from "~/types/api";

const MAX_ITEMS = 30;

/** Data local de hoje em "yyyy-MM-dd" para min/validação do campo de entrega. */
function todayLocal(): string {
  const date = new Date();
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function formatQuantity(value: number): string {
  return new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 }).format(
    value,
  );
}

const itemSchema = z.object({
  productId: z.string().min(1, "Selecione um produto"),
  quantity: z.coerce.number().min(0.01, "Quantidade mínima 0,01"),
});

const orderSchema = z
  .object({
    deliveryDate: z.string().min(1, "Informe a data de entrega"),
    items: z.array(itemSchema).min(1, "Adicione pelo menos um item"),
  })
  .refine((data) => data.deliveryDate >= todayLocal(), {
    message: "A data de entrega não pode ser anterior a hoje",
    path: ["deliveryDate"],
  });

type OrderFormInput = z.input<typeof orderSchema>;
type OrderFormValues = z.output<typeof orderSchema>;

export default function NewOrderPage() {
  const { isProduction } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const productsQuery = useQuery({
    queryKey: ["products", "active"],
    queryFn: () => listProducts({ active: true, page: 0, size: 100 }),
  });

  const products = productsQuery.data?.content ?? [];

  const {
    register,
    handleSubmit,
    control,
    watch,
    formState: { errors },
  } = useForm<OrderFormInput, unknown, OrderFormValues>({
    resolver: zodResolver(orderSchema),
    defaultValues: {
      deliveryDate: "",
      items: [{ productId: "", quantity: 1 }],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: "items",
  });

  const watchedItems = watch("items");
  const itemsValues = watchedItems ?? [];

  const productById = useMemo(() => {
    const map = new Map<string, ProductResponse>();
    for (const product of products) {
      map.set(product.id, product);
    }
    return map;
  }, [products]);

  // Produtos já selecionados em OUTRAS linhas (evita duplicar o mesmo produto).
  const excludedProductIds = useMemo(() => {
    const set = new Set<string>();
    for (const item of itemsValues) {
      if (item.productId) set.add(item.productId);
    }
    return set;
  }, [itemsValues]);

  const rows = itemsValues.map((item, index) => {
    const product = item.productId ? productById.get(item.productId) : undefined;
    const quantity = Number(item.quantity) || 0;
    const subtotal = product ? product.unitPrice * quantity : 0;
    return {
      key: fields[index]?.id ?? index,
      product,
      quantity,
      subtotal,
    };
  });

  const totalAmount = rows.reduce((sum, row) => sum + row.subtotal, 0);

  const createMutation = useMutation({
    mutationFn: (payload: { deliveryDate: string; items: { productId: string; quantity: number }[] }) =>
      createOrder(payload),
    onSuccess: (order) => {
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      navigate(`/orders/${order.id}`);
    },
  });

  const onSubmit = (values: OrderFormValues) => {
    createMutation.mutate({
      deliveryDate: values.deliveryDate,
      items: values.items.map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
      })),
    });
  };

  const itemsListError =
    errors.items?.root?.message ?? errors.items?.message ?? undefined;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Novo Pedido"
        subtitle="Informe a data de entrega e escolha os itens do pedido."
        actions={
          <Link to="/orders" className="btn btn-ghost">
            Voltar
          </Link>
        }
      />

      {isProduction ? (
        <div role="alert" className="alert alert-warning sm:alert-horizontal">
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
              d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126ZM12 15.75h.007v.008H12v-.008Z"
            />
          </svg>
          <span className="flex-1">
            Apenas solicitantes e administradores podem criar pedidos.
          </span>
        </div>
      ) : productsQuery.isLoading ? (
        <div className="flex items-center justify-center py-16">
          <span
            role="status"
            aria-label="Carregando"
            className="loading loading-spinner loading-lg"
          />
        </div>
      ) : productsQuery.isError ? (
        <ErrorAlert
          error={getApiErrorMessage(productsQuery.error)}
          onRetry={() => productsQuery.refetch()}
        />
      ) : products.length === 0 ? (
        <EmptyState
          message="Nenhum produto ativo disponível."
          hint="Peça ao administrador para cadastrar produtos."
        />
      ) : (
        <form
          onSubmit={handleSubmit(onSubmit)}
          noValidate
          className="space-y-6"
        >
          {createMutation.isError ? (
            <ErrorAlert error={getApiErrorMessage(createMutation.error)} />
          ) : null}

          <div className="card border border-base-300 bg-base-100">
            <div className="card-body">
              <h2 className="card-title">Entrega</h2>
              <div className="max-w-xs">
                <FormField
                  label="Data de entrega"
                  required
                  error={errors.deliveryDate?.message}
                >
                  <input
                    type="date"
                    min={todayLocal()}
                    className="input w-full"
                    {...register("deliveryDate")}
                  />
                </FormField>
              </div>
            </div>
          </div>

          <div className="card border border-base-300 bg-base-100">
            <div className="card-body">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <h2 className="card-title">Itens do pedido</h2>
                <button
                  type="button"
                  className="btn btn-outline btn-sm"
                  onClick={() => append({ productId: "", quantity: 1 })}
                  disabled={fields.length >= MAX_ITEMS}
                >
                  Adicionar item
                </button>
              </div>

              <div className="space-y-4">
                {fields.map((field, index) => {
                  const row = rows[index];
                  const excludeIds = new Set(
                    Array.from(excludedProductIds).filter(
                      (id) => id !== itemsValues[index]?.productId,
                    ),
                  );
                  return (
                    <div
                      key={field.id}
                      className="rounded-box border border-base-200 p-3"
                    >
                      <div className="grid gap-3 md:grid-cols-12">
                        <div className="md:col-span-6">
                          <FormField
                            label="Produto"
                            required
                            error={errors.items?.[index]?.productId?.message}
                          >
                            <select
                              className="select w-full"
                              {...register(`items.${index}.productId` as const)}
                            >
                              <option value="">Selecione um produto</option>
                              {products.map((product) => (
                                <option
                                  key={product.id}
                                  value={product.id}
                                  disabled={excludeIds.has(product.id)}
                                >
                                  {product.name} —{" "}
                                  {formatCurrency(product.unitPrice)} /{" "}
                                  {UNIT_MEASURE_LABEL[product.unitMeasure]}
                                </option>
                              ))}
                            </select>
                          </FormField>
                        </div>

                        <div className="md:col-span-3">
                          <FormField
                            label="Quantidade"
                            required
                            error={errors.items?.[index]?.quantity?.message}
                          >
                            <input
                              type="number"
                              step="0.01"
                              min="0.01"
                              className="input w-full"
                              {...register(`items.${index}.quantity`, {
                                valueAsNumber: true,
                              })}
                            />
                          </FormField>
                        </div>

                        <div className="flex flex-col justify-end md:col-span-2">
                          <span className="text-sm font-medium text-base-content">
                            Subtotal
                          </span>
                          <span className="text-base-content/80">
                            {formatCurrency(row?.subtotal ?? 0)}
                          </span>
                        </div>

                        <div className="flex items-end justify-end md:col-span-1">
                          <button
                            type="button"
                            className="btn btn-ghost btn-square btn-sm"
                            aria-label={`Remover item ${index + 1}`}
                            onClick={() => remove(index)}
                            disabled={fields.length <= 1}
                          >
                            <svg
                              xmlns="http://www.w3.org/2000/svg"
                              fill="none"
                              viewBox="0 0 24 24"
                              strokeWidth={1.5}
                              stroke="currentColor"
                              className="h-5 w-5"
                              aria-hidden="true"
                            >
                              <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                d="M6 18 18 6M6 6l12 12"
                              />
                            </svg>
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>

              {itemsListError ? (
                <span className="text-sm text-error">{itemsListError}</span>
              ) : null}
              {fields.length >= MAX_ITEMS ? (
                <span className="text-sm text-base-content/60">
                  Limite de {MAX_ITEMS} itens por pedido atingido.
                </span>
              ) : null}
            </div>
          </div>

          <div className="card border border-base-300 bg-base-100">
            <div className="card-body">
              <h2 className="card-title">Resumo</h2>
              <div className="overflow-x-auto">
                <table className="table table-sm">
                  <thead>
                    <tr>
                      <th>Produto</th>
                      <th className="text-right">Quantidade</th>
                      <th className="text-right">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row) => (
                      <tr key={row.key}>
                        <td>{row.product ? row.product.name : "—"}</td>
                        <td className="text-right">
                          {formatQuantity(row.quantity)}
                        </td>
                        <td className="text-right">
                          {formatCurrency(row.subtotal)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                  <tfoot>
                    <tr>
                      <th className="font-bold">Total</th>
                      <th />
                      <th className="text-right font-bold">
                        {formatCurrency(totalAmount)}
                      </th>
                    </tr>
                  </tfoot>
                </table>
              </div>
            </div>
          </div>

          <div className="flex flex-wrap justify-end gap-2">
            <button
              type="button"
              className="btn btn-ghost"
              onClick={() => navigate("/orders")}
            >
              Cancelar
            </button>
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
              Criar pedido
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
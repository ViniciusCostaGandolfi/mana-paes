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
import { getApiErrorMessage } from "~/lib/api";
import { formatCurrency, formatPhoneBR, ORDER_SHIFT_FIELDS, UNIT_MEASURE_LABEL } from "~/lib/utils";
import { createOrder } from "~/services/order.service";
import { listProducts } from "~/services/product.service";
import { listVendors } from "~/services/vendor.service";
import type { OrderItemRequest, ProductResponse } from "~/types/api";

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

const selectionSchema = z.object({
  groupId: z.string(),
  optionId: z.string(),
});

const itemSchema = z
  .object({
    productId: z.string().min(1, "Selecione um produto"),
    quantidadeManha: z.coerce.number().min(0, "Não pode ser negativo."),
    quantidadeTarde: z.coerce.number().min(0, "Não pode ser negativo."),
    quantidadeNoite: z.coerce.number().min(0, "Não pode ser negativo."),
    selections: z.array(selectionSchema).optional(),
  })
  .refine(
    (data) =>
      data.quantidadeManha + data.quantidadeTarde + data.quantidadeNoite > 0,
    {
      message: "Informe ao menos um turno com quantidade.",
      path: ["quantidadeManha"],
    },
  );

const orderSchema = z
  .object({
    deliveryDate: z.string().min(1, "Informe a data de entrega"),
    vendorId: z.string().optional(),
    items: z.array(itemSchema).min(1, "Adicione pelo menos um item"),
  })
  .refine((data) => data.deliveryDate >= todayLocal(), {
    message: "A data de entrega não pode ser anterior a hoje",
    path: ["deliveryDate"],
  });

type OrderFormInput = z.input<typeof orderSchema>;
type OrderFormValues = z.output<typeof orderSchema>;

interface OrderItemFormValue {
  productId: string;
  quantidadeManha: number;
  quantidadeTarde: number;
  quantidadeNoite: number;
  selections?: { groupId: string; optionId: string }[];
}

export default function NewOrderPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const productsQuery = useQuery({
    queryKey: ["products", "active"],
    queryFn: () => listProducts({ active: true, page: 0, size: 100 }),
  });
  const vendorsQuery = useQuery({
    queryKey: ["vendors"],
    queryFn: listVendors,
  });

  const products = productsQuery.data?.content ?? [];
  const vendors = vendorsQuery.data ?? [];

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<OrderFormInput, unknown, OrderFormValues>({
    resolver: zodResolver(orderSchema),
    defaultValues: {
      deliveryDate: "",
      vendorId: "",
      items: [
        {
          productId: "",
          quantidadeManha: 0,
          quantidadeTarde: 0,
          quantidadeNoite: 0,
          selections: [],
        } as OrderItemFormValue,
      ],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control,
    name: "items",
  });

  const watchedItems = watch("items");
  const itemsValues = (watchedItems ?? []) as OrderItemFormValue[];

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

  const handleProductChange = (index: number, productId: string) => {
    const product = productById.get(productId);
    const selections = (product?.optionGroups ?? []).map((group) => ({
      groupId: group.id,
      optionId: "",
    }));
    setValue(`items.${index}.selections`, selections as never);
  };

  const handleOptionChange = (
    index: number,
    groupId: string,
    optionId: string,
  ) => {
    const current = itemsValues[index]?.selections ?? [];
    const next = current.map((selection) =>
      selection.groupId === groupId ? { ...selection, optionId } : selection,
    );
    setValue(`items.${index}.selections`, next as never);
  };

  const rows = itemsValues.map((item, index) => {
    const product = item.productId ? productById.get(item.productId) : undefined;
    const shifts = {
      quantidadeManha: Number(item.quantidadeManha) || 0,
      quantidadeTarde: Number(item.quantidadeTarde) || 0,
      quantidadeNoite: Number(item.quantidadeNoite) || 0,
    };
    const totalQuantity =
      shifts.quantidadeManha + shifts.quantidadeTarde + shifts.quantidadeNoite;
    const optionAdjustment = (item.selections ?? [])
      .map((selection) => {
        if (!selection.optionId) return 0;
        const group = product?.optionGroups.find(
          (g) => g.id === selection.groupId,
        );
        const option = group?.options.find((o) => o.id === selection.optionId);
        return option?.priceAdjustment ?? 0;
      })
      .reduce((sum, value) => sum + value, 0);
    const unitPrice = product ? product.unitPrice + optionAdjustment : 0;
    const subtotal = unitPrice * totalQuantity;
    return { key: fields[index]?.id ?? index, product, totalQuantity, subtotal };
  });

  const totalAmount = rows.reduce((sum, row) => sum + row.subtotal, 0);

  const createMutation = useMutation({
    mutationFn: (payload: {
      deliveryDate: string;
      vendorId: string | null;
      items: OrderItemRequest[];
    }) => createOrder(payload),
    onSuccess: (order) => {
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      navigate(`/orders/${order.id}`);
    },
  });

  const onSubmit = (values: OrderFormValues) => {
    createMutation.mutate({
      deliveryDate: values.deliveryDate,
      vendorId: values.vendorId?.trim() || null,
      items: values.items.map((item) => ({
        productId: item.productId,
        quantidadeManha: item.quantidadeManha,
        quantidadeTarde: item.quantidadeTarde,
        quantidadeNoite: item.quantidadeNoite,
        selections: (item.selections ?? [])
          .filter((selection) => selection.groupId && selection.optionId)
          .map((selection) => ({
            groupId: selection.groupId,
            optionId: selection.optionId,
          })),
      })),
    });
  };

  const itemsListError =
    errors.items?.root?.message ?? errors.items?.message ?? undefined;

  return (
    <div className="space-y-6">
      <PageHeader
        title="Novo Pedido"
        subtitle="Informe a data de entrega, o vendedor (opcional) e os itens com quantidades por turno."
        actions={
          <Link to="/orders" className="btn btn-ghost">
            Voltar
          </Link>
        }
      />

      {productsQuery.isLoading || vendorsQuery.isLoading ? (
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
      ) : vendorsQuery.isError ? (
        <ErrorAlert
          error={getApiErrorMessage(vendorsQuery.error)}
          onRetry={() => vendorsQuery.refetch()}
        />
      ) : products.length === 0 ? (
        <EmptyState
          message="Nenhum produto ativo disponível."
          hint="Cadastre produtos antes de criar pedidos."
        />
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-6">
          {createMutation.isError ? (
            <ErrorAlert error={getApiErrorMessage(createMutation.error)} />
          ) : null}

          <div className="card border border-base-300 bg-base-100">
            <div className="card-body">
              <h2 className="card-title">Entrega</h2>
              <div className="grid gap-4 sm:grid-cols-2">
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
                <div className="max-w-xs">
                  <FormField
                    label="Vendedor"
                    hint="Opcional. Associa o pedido a um vendedor cadastrado."
                  >
                    <select className="select w-full" {...register("vendorId")}>
                      <option value="">Sem vendedor</option>
                      {vendors.map((vendor) => (
                        <option key={vendor.id} value={vendor.id}>
                          {vendor.name} ({formatPhoneBR(vendor.phone)})
                        </option>
                      ))}
                    </select>
                  </FormField>
                </div>
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
                  onClick={() =>
                    append({
                      productId: "",
                      quantidadeManha: 0,
                      quantidadeTarde: 0,
                      quantidadeNoite: 0,
                      selections: [],
                    } as OrderItemFormValue)
                  }
                  disabled={fields.length >= MAX_ITEMS}
                >
                  Adicionar item
                </button>
              </div>

              <div className="space-y-4">
                {fields.map((field, index) => {
                  const item = itemsValues[index];
                  const product = item?.productId
                    ? productById.get(item.productId)
                    : undefined;
                  const excludeIds = new Set(
                    Array.from(excludedProductIds).filter(
                      (id) => id !== item?.productId,
                    ),
                  );
                  const selections = item?.selections ?? [];
                  return (
                    <div
                      key={field.id}
                      className="rounded-box border border-base-200 p-3"
                    >
                      <div className="grid gap-3 md:grid-cols-12">
                        <div className="md:col-span-4">
                          <FormField
                            label="Produto"
                            required
                            error={errors.items?.[index]?.productId?.message}
                          >
                            <select
                              className="select w-full"
                              {...register(`items.${index}.productId` as const, {
                                onChange: (event) =>
                                  handleProductChange(index, event.target.value),
                              })}
                            >
                              <option value="">Selecione um produto</option>
                              {products.map((productOption) => (
                                <option
                                  key={productOption.id}
                                  value={productOption.id}
                                  disabled={excludeIds.has(productOption.id)}
                                >
                                  {productOption.name} —{" "}
                                  {formatCurrency(productOption.unitPrice)} /{" "}
                                  {UNIT_MEASURE_LABEL[productOption.unitMeasure]}
                                </option>
                              ))}
                            </select>
                          </FormField>
                        </div>

                        {ORDER_SHIFT_FIELDS.map((shift, shiftIndex) => (
                          <div key={shift.key} className="md:col-span-2">
                            <FormField
                              label={shift.label}
                              error={
                                errors.items?.[index]?.[shift.key]?.message ??
                                (shiftIndex === 0
                                  ? errors.items?.[index]?.quantidadeManha?.message
                                  : undefined)
                              }
                            >
                              <input
                                type="number"
                                step="0.01"
                                min="0"
                                className="input w-full"
                                placeholder="0"
                                {...register(`items.${index}.${shift.key}` as const, {
                                  valueAsNumber: true,
                                })}
                              />
                            </FormField>
                          </div>
                        ))}

                        <div className="flex items-end justify-end md:col-span-2">
                          <div>
                            <span className="text-sm font-medium text-base-content">
                              Subtotal
                            </span>
                            <span className="block text-base-content/80">
                              {formatCurrency(rows[index]?.subtotal ?? 0)}
                            </span>
                          </div>
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

                      {product && product.optionGroups.length > 0 ? (
                        <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                          {product.optionGroups.map((group) => {
                            const selected = selections.find(
                              (selection) => selection.groupId === group.id,
                            );
                            return (
                              <div key={group.id}>
                                <FormField
                                  label={group.name}
                                  hint={
                                    group.required
                                      ? "Obrigatório"
                                      : "Opcional"
                                  }
                                >
                                  <select
                                    className="select select-sm w-full"
                                    value={selected?.optionId ?? ""}
                                    onChange={(event) =>
                                      handleOptionChange(
                                        index,
                                        group.id,
                                        event.target.value,
                                      )
                                    }
                                  >
                                    <option value="">
                                      {group.required ? "Selecione..." : "Nenhuma"}
                                    </option>
                                    {group.options.map((option) => (
                                      <option key={option.id} value={option.id}>
                                        {option.name}
                                        {option.priceAdjustment > 0
                                          ? ` (+${formatCurrency(option.priceAdjustment)})`
                                          : ""}
                                      </option>
                                    ))}
                                  </select>
                                </FormField>
                              </div>
                            );
                          })}
                        </div>
                      ) : null}
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
                      <th className="text-right">Total</th>
                      <th className="text-right">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody>
                    {rows.map((row) => (
                      <tr key={row.key}>
                        <td>{row.product ? row.product.name : "—"}</td>
                        <td className="text-right">
                          {formatQuantity(row.totalQuantity)}
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
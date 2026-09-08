import { useFieldArray, type Control, type FieldErrors, type UseFormRegister } from "react-hook-form";
import { FormField } from "~/components/ui/form-field";
import type { UnitMeasure } from "~/types/api";

export interface OptionGroupEditorOption {
  name: string;
  priceAdjustment: string;
}
export interface OptionGroupEditorRow {
  name: string;
  options: OptionGroupEditorOption[];
}

/**
 * Tipo do formulário de produto utilizado pelas telas de criação/edição.
 * Campos numéricos permanecem como string no form e são convertidos no submit.
 */
export interface ProductFormValues {
  name: string;
  description?: string;
  unitPrice: string;
  unitMeasure: UnitMeasure;
  optionGroups?: OptionGroupEditorRow[];
}

interface OptionGroupRowProps {
  control: Control<ProductFormValues>;
  register: UseFormRegister<ProductFormValues>;
  errors: FieldErrors<ProductFormValues>;
  groupIndex: number;
  canRemove: boolean;
  onRemove: () => void;
}

function OptionGroupRow({
  control,
  register,
  errors,
  groupIndex,
  canRemove,
  onRemove,
}: OptionGroupRowProps) {
  const { fields: options, append: appendOption, remove: removeOption } = useFieldArray({
    control,
    name: `optionGroups.${groupIndex}.options`,
  });

  const optionsRootError = errors.optionGroups?.[groupIndex]?.options?.root?.message;

  return (
    <div className="rounded-box border border-base-200 p-3">
      <div className="flex items-center gap-2">
        <FormField
          label={`Grupo ${groupIndex + 1}`}
          required
          error={errors.optionGroups?.[groupIndex]?.name?.message}
        >
          <input
            type="text"
            className="input input-sm w-full"
            placeholder="Ex.: Tamanho"
            {...register(`optionGroups.${groupIndex}.name`)}
          />
        </FormField>
        <button
          type="button"
          className="btn btn-ghost btn-square btn-sm"
          aria-label={`Remover grupo ${groupIndex + 1}`}
          onClick={onRemove}
          disabled={canRemove}
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
            <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <div className="mt-3 space-y-2">
        {options.map((option, optionIndex) => (
          <div key={option.id} className="flex items-end gap-2">
            <div className="flex-1">
              <FormField
                label={`Opção ${optionIndex + 1}`}
                required
                error={errors.optionGroups?.[groupIndex]?.options?.[optionIndex]?.name?.message}
              >
                <input
                  type="text"
                  className="input input-sm w-full"
                  placeholder="Ex.: Grande"
                  {...register(`optionGroups.${groupIndex}.options.${optionIndex}.name`)}
                />
              </FormField>
            </div>
            <div className="w-36">
              <FormField
                label="Acréscimo"
                error={
                  errors.optionGroups?.[groupIndex]?.options?.[optionIndex]?.priceAdjustment
                    ?.message
                }
              >
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  className="input input-sm w-full"
                  placeholder="0,00"
                  {...register(`optionGroups.${groupIndex}.options.${optionIndex}.priceAdjustment`)}
                />
              </FormField>
            </div>
            <button
              type="button"
              className="btn btn-ghost btn-square btn-sm"
              aria-label={`Remover opção ${optionIndex + 1} do grupo ${groupIndex + 1}`}
              onClick={() => removeOption(optionIndex)}
              disabled={options.length <= 1}
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
                <path strokeLinecap="round" strokeLinejoin="round" d="M6 18 18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        ))}
      </div>

      {optionsRootError ? (
        <span className="text-sm text-error">{optionsRootError}</span>
      ) : null}

      <div className="mt-2">
        <button
          type="button"
          className="btn btn-ghost btn-sm"
          onClick={() => appendOption({ name: "", priceAdjustment: "0" })}
        >
          Adicionar opção
        </button>
      </div>
    </div>
  );
}

interface OptionGroupsEditorProps {
  control: Control<ProductFormValues>;
  register: UseFormRegister<ProductFormValues>;
  errors: FieldErrors<ProductFormValues>;
}

/**
 * Editor de grupos de opções de um produto (useFieldArray aninhado).
 * Integra com react-hook-form no campo `optionGroups` do formulário pai.
 */
export function OptionGroupsEditor({
  control,
  register,
  errors,
}: OptionGroupsEditorProps) {
  const { fields: groups, append: appendGroup, remove: removeGroup } = useFieldArray({
    control,
    name: "optionGroups",
  });

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h3 className="font-medium text-base-content">Grupos de opções</h3>
          <p className="text-sm text-base-content/60">
            Opcional. Cada grupo tem uma ou mais opções com acréscimo de preço.
          </p>
        </div>
        <button
          type="button"
          className="btn btn-outline btn-sm"
          onClick={() => appendGroup({ name: "", options: [{ name: "", priceAdjustment: "0" }] })}
        >
          Adicionar grupo
        </button>
      </div>

      {groups.length === 0 ? (
        <p className="text-sm text-base-content/50">
          Nenhum grupo cadastrado — o produto será vendido sem opções.
        </p>
      ) : null}

      {groups.map((group, groupIndex) => (
        <OptionGroupRow
          key={group.id}
          control={control}
          register={register}
          errors={errors}
          groupIndex={groupIndex}
          canRemove={groups.length <= 1}
          onRemove={() => removeGroup(groupIndex)}
        />
      ))}
    </div>
  );
}

export default OptionGroupsEditor;
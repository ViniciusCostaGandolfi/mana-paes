import type { ReactNode } from "react";

interface FormFieldProps {
  label: string;
  required?: boolean;
  error?: string;
  hint?: string;
  children: ReactNode;
}

export function FormField({
  label,
  required = false,
  error,
  hint,
  children,
}: FormFieldProps) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-sm font-medium text-base-content">
        {label}
        {required ? (
          <span className="text-error" aria-hidden="true">
            {" "}
            *
          </span>
        ) : null}
      </span>
      {children}
      {error ? (
        <span className="text-sm text-error">{error}</span>
      ) : hint ? (
        <span className="text-sm text-base-content/60">{hint}</span>
      ) : null}
    </div>
  );
}

export default FormField;
import { cn } from "~/lib/utils";

type SpinnerSize = "sm" | "md" | "lg";

const sizeClass: Record<SpinnerSize, string> = {
  sm: "loading-sm",
  md: "loading-md",
  lg: "loading-lg",
};

interface SpinnerProps {
  size?: SpinnerSize;
}

export function Spinner({ size = "md" }: SpinnerProps) {
  return (
    <span
      role="status"
      aria-label="Carregando"
      className={cn("loading loading-spinner", sizeClass[size])}
    />
  );
}

export default Spinner;
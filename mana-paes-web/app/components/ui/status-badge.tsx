import type { OrderStatus } from "~/types/api";
import {
  ORDER_STATUS_BADGE_CLASS,
  ORDER_STATUS_LABEL,
  cn,
} from "~/lib/utils";

interface StatusBadgeProps {
  status: OrderStatus;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span className={cn("badge", ORDER_STATUS_BADGE_CLASS[status])}>
      {ORDER_STATUS_LABEL[status]}
    </span>
  );
}

export default StatusBadge;
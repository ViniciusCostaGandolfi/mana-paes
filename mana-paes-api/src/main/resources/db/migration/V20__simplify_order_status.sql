-- V20: simplificar OrderStatus para PENDING/ACCEPTED/CANCELLED.
-- Remove IN_PRODUCTION/READY/DELIVERED mapeando os dados existentes para ACCEPTED.
UPDATE orders SET status = 'ACCEPTED' WHERE status IN ('IN_PRODUCTION','READY','DELIVERED');
UPDATE order_status_history SET new_status = 'ACCEPTED' WHERE new_status IN ('IN_PRODUCTION','READY','DELIVERED');
UPDATE order_status_history SET previous_status = 'ACCEPTED' WHERE previous_status IN ('IN_PRODUCTION','READY','DELIVERED');
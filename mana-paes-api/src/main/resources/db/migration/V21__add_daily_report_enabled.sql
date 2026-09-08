-- V21: notification_configs ganha flag para ativar/desativar o envio automatico
-- do relatorio diario (DailyReportScheduler).
ALTER TABLE notification_configs ADD COLUMN daily_report_enabled BOOLEAN NOT NULL DEFAULT TRUE;
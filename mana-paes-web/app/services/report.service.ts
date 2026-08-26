import { api } from "~/lib/api";
import type {
  DailyFinancialReportResponse,
  DailyProductionReportResponse,
  DailyReportDispatchResponse,
} from "~/types/api";

export const getDailyProduction = async (
  date: string,
): Promise<DailyProductionReportResponse> => {
  const { data } = await api.get<DailyProductionReportResponse>("/reports/daily/production", {
    params: { date },
  });
  return data;
};

export const getDailyFinancial = async (
  date: string,
): Promise<DailyFinancialReportResponse> => {
  const { data } = await api.get<DailyFinancialReportResponse>("/reports/daily/financial", {
    params: { date },
  });
  return data;
};

export const sendDailyReport = async (date: string): Promise<DailyReportDispatchResponse> => {
  const { data } = await api.post<DailyReportDispatchResponse>(
    "/notifications/reports/daily/send",
    null,
    { params: { date } },
  );
  return data;
};
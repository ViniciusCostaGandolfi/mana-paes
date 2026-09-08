import { api } from "~/lib/api";
import type { ProductResponse } from "~/types/api";

/**
 * Catálogo público de produtos ativos do tenant do vendedor (permitAll,
 * sem autenticação). O telefone resolve o vendedor no backend; se não
 * cadastrado/inativo, o backend responde 400.
 */
export const listPublicProducts = async (
  phone: string,
): Promise<ProductResponse[]> => {
  const { data } = await api.get<ProductResponse[]>(
    `/public/products?phone=${encodeURIComponent(phone)}`,
  );
  return data;
};
interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  size: number;
  onChange: (page: number) => void;
}

export function Pagination({
  page,
  totalPages,
  totalElements,
  size,
  onChange,
}: PaginationProps) {
  const start = totalElements === 0 ? 0 : (page - 1) * size + 1;
  const end = totalElements === 0 ? 0 : Math.min(page * size, totalElements);

  return (
    <div className="flex flex-col items-center justify-between gap-3 sm:flex-row">
      <p className="text-sm text-base-content/70">
        Mostrando {start}–{end} de {totalElements}
      </p>
      <div className="join">
        <button
          type="button"
          className="btn btn-sm join-item"
          disabled={page <= 1}
          onClick={() => onChange(page - 1)}
        >
          Anterior
        </button>
        <span
          className="btn btn-sm join-item btn-ghost pointer-events-none"
          aria-current="page"
        >
          Página {page} de {totalPages}
        </span>
        <button
          type="button"
          className="btn btn-sm join-item"
          disabled={page >= totalPages}
          onClick={() => onChange(page + 1)}
        >
          Próxima
        </button>
      </div>
    </div>
  );
}

export default Pagination;
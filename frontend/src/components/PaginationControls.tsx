interface PaginationControlsProps {
  page: number;
  size: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

function PaginationControls({ page, size, totalPages, onPageChange }: PaginationControlsProps) {
  const prevDisabled = page <= 0;
  const nextDisabled = page + 1 >= totalPages;

  return (
    <div className="flex items-center justify-between border-t border-slate-800 pt-4 text-sm text-slate-300">
      <span>
        Page {totalPages === 0 ? 0 : page + 1} of {totalPages}
      </span>
      <div className="flex items-center gap-3">
        <button
          type="button"
          disabled={prevDisabled}
          onClick={() => onPageChange(Math.max(page - 1, 0))}
          className="rounded border border-slate-700 px-3 py-1 font-semibold text-slate-200 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Previous
        </button>
        <button
          type="button"
          disabled={nextDisabled}
          onClick={() => onPageChange(page + 1)}
          className="rounded border border-slate-700 px-3 py-1 font-semibold text-slate-200 hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Next
        </button>
      </div>
    </div>
  );
}

export default PaginationControls;


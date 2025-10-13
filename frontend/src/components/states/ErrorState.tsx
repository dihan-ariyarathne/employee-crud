interface ErrorStateProps {
  message?: string;
  onRetry?: () => void;
}

function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="flex items-center justify-between gap-4 rounded border border-rose-500/60 bg-rose-950/20 p-4 text-sm text-rose-200">
      <span>{message ?? 'Something went wrong.'}</span>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="rounded border border-rose-400 px-3 py-1 text-xs font-semibold text-rose-100 hover:bg-rose-400 hover:text-rose-900"
        >
          Retry
        </button>
      ) : null}
    </div>
  );
}

export default ErrorState;


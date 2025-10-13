import { useState } from 'react';

import type { SchemaField } from '../types/schema.ts';

interface FiltersBarProps {
  fields: SchemaField[];
  search: string;
  activeFilters: Record<string, string>;
  onSearchChange: (value: string) => void;
  onFiltersChange: (filters: Record<string, string>) => void;
}

function FiltersBar({ fields, search, activeFilters, onSearchChange, onFiltersChange }: FiltersBarProps) {
  const [selectedField, setSelectedField] = useState<string>(fields[0]?.name ?? '');
  const [operator, setOperator] = useState<string>('contains');
  const [value, setValue] = useState<string>('');

  const handleAddFilter = () => {
    if (!selectedField || !value) return;
    onFiltersChange({
      ...activeFilters,
      [selectedField]: `${operator}:${value}`
    });
    setValue('');
  };

  const handleRemoveFilter = (field: string) => {
    const { [field]: _, ...rest } = activeFilters;
    onFiltersChange(rest);
  };

  return (
    <div className="rounded border border-slate-800 bg-slate-950/60 p-4">
      {/* Inner padding to align with table cell padding (px-4) */}
      <div className="px-4">
        {/* Row 1: Search full width */}
        <div className="mb-4 flex flex-col gap-2">
          <label htmlFor="search" className="text-xs font-semibold uppercase text-slate-400">
            Search
          </label>
          <input
            id="search"
            type="text"
            value={search}
            onChange={(event) => onSearchChange(event.target.value)}
            placeholder="Search across string fields..."
            className="w-full rounded border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100"
          />
        </div>

        {/* Row 2: Filters (equal four columns across the card) */}
        <div className="mt-4">
          <label htmlFor="field" className="mb-2 block text-xs font-semibold uppercase text-slate-400">
            Add Filter
          </label>
          <div className="flex w-full flex-col gap-2 sm:flex-row sm:items-end sm:gap-3">
            <select
              id="field"
              value={selectedField}
              onChange={(event) => setSelectedField(event.target.value)}
              className="w-full min-w-0 rounded border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 sm:flex-1"
            >
              {fields.map((field) => (
                <option key={field.name} value={field.name}>
                  {field.name}
                </option>
              ))}
            </select>
            <select
              value={operator}
              onChange={(event) => setOperator(event.target.value)}
              className="w-full min-w-0 rounded border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 sm:flex-1"
            >
              <option value="contains">Contains</option>
              <option value="eq">Equals</option>
              <option value="gt">Greater Than</option>
              <option value="lt">Less Than</option>
            </select>
            <input
              type="text"
              value={value}
              onChange={(event) => setValue(event.target.value)}
              className="w-full min-w-0 rounded border border-slate-700 bg-slate-900 px-3 py-2 text-sm text-slate-100 sm:flex-1"
              placeholder="Value"
            />
            <button
              type="button"
              onClick={handleAddFilter}
              className="h-10 w-full shrink-0 rounded bg-sky-500 px-4 text-sm font-semibold text-slate-950 hover:bg-sky-400 sm:w-28"
            >
              Apply
            </button>
          </div>
        </div>

        {/* Row 3: Active filter chips */}
        {Object.entries(activeFilters).length > 0 ? (
          <div className="mt-4 flex flex-wrap gap-2">
            {Object.entries(activeFilters).map(([field, filterValue]) => (
              <span
                key={field}
                className="inline-flex items-center gap-2 rounded-full border border-sky-400/40 bg-sky-400/10 px-3 py-1 text-xs text-sky-200"
              >
                {field}: {filterValue}
                <button type="button" onClick={() => handleRemoveFilter(field)} className="text-sky-300 hover:text-sky-100">
                  Ã—
                </button>
              </span>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}

export default FiltersBar;

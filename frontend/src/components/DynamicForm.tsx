import { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import clsx from 'clsx';

import type { SchemaField, SchemaFieldType } from '../types/schema.ts';

interface DynamicFormProps {
  fields: SchemaField[];
  defaultValues?: Record<string, unknown>;
  submitLabel?: string;
  onSubmit: (values: Record<string, unknown>) => void;
  onCancel?: () => void;
}

type FormValues = Record<string, unknown>;

function fieldOrderComparator(a: SchemaField, b: SchemaField) {
  if (a.required && !b.required) return -1;
  if (!a.required && b.required) return 1;
  return a.name.localeCompare(b.name);
}

function normalizeDefaultValue(field: SchemaField, value: unknown) {
  if (value === undefined || value === null) return field.type === 'BOOLEAN' ? false : '';
  switch (field.type) {
    case 'DATE': {
      const date = new Date(String(value));
      if (Number.isNaN(date.getTime())) return '';
      return date.toISOString().split('T')[0];
    }
    case 'BOOLEAN':
      return Boolean(value);
    case 'ARRAY':
    case 'OBJECT':
      return JSON.stringify(value, null, 2);
    default:
      return value;
  }
}

function DynamicForm({ fields, defaultValues, submitLabel = 'Save', onSubmit, onCancel }: DynamicFormProps) {
  const sortedFields = [...fields].sort(fieldOrderComparator);
  const form = useForm<FormValues>({
    defaultValues: sortedFields.reduce<FormValues>((acc, field) => {
      acc[field.name] = normalizeDefaultValue(field, defaultValues?.[field.name]);
      return acc;
    }, {})
  });

  useEffect(() => {
    if (!defaultValues) return;
    sortedFields.forEach((field) => {
      form.setValue(field.name, normalizeDefaultValue(field, defaultValues[field.name]));
    });
  }, [defaultValues, form, sortedFields]);

  const renderField = (field: SchemaField) => {
    const commonProps = {
      id: field.name,
      ...form.register(field.name, { required: field.required })
    };
    switch (field.type) {
      case 'BOOLEAN':
        return (
          <input
            type="checkbox"
            className="h-4 w-4 rounded border-slate-700 bg-slate-900 text-sky-500"
            {...form.register(field.name)}
            defaultChecked={Boolean(defaultValues?.[field.name])}
          />
        );
      case 'NUMBER':
        return (
          <input
            type="number"
            step="any"
            className="w-full rounded border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            {...commonProps}
          />
        );
      case 'DATE':
        return (
          <input
            type="date"
            className="w-full rounded border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            {...commonProps}
          />
        );
      case 'ARRAY':
      case 'OBJECT':
        return (
          <textarea
            rows={3}
            className="w-full rounded border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            placeholder="Enter JSON"
            {...commonProps}
          />
        );
      case 'STRING':
      case 'NULL':
      case 'UNKNOWN':
      default:
        return (
          <input
            type="text"
            className="w-full rounded border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100"
            {...commonProps}
          />
        );
    }
  };

  const convertValues = (values: FormValues) => {
    const converted: Record<string, unknown> = {};
    sortedFields.forEach((field) => {
      const rawValue = values[field.name];
      converted[field.name] = convertValue(field, rawValue);
    });
    return converted;
  };

  const convertValue = (field: SchemaField, value: unknown) => {
    switch (field.type) {
      case 'BOOLEAN':
        return Boolean(value);
      case 'NUMBER': {
        const num = Number(value);
        return Number.isNaN(num) ? value : num;
      }
      case 'DATE':
        if (!value) return null;
        return new Date(String(value)).toISOString();
      case 'ARRAY':
      case 'OBJECT':
        if (!value) return field.type === 'ARRAY' ? [] : {};
        try {
          return JSON.parse(String(value));
        } catch {
          return value;
        }
      default:
        return value;
    }
  };

  const handleSubmit = (values: FormValues) => {
    onSubmit(convertValues(values));
    form.reset();
  };

  return (
    <form className="space-y-4" onSubmit={form.handleSubmit(handleSubmit)}>
      <div className="grid gap-4 sm:grid-cols-2">
        {sortedFields.map((field) => (
          <div key={field.name} className={clsx(field.type === 'BOOLEAN' && 'flex items-center gap-3')}>
            <label htmlFor={field.name} className="block text-sm font-medium text-slate-300">
              {field.name}
              {field.required ? <span className="text-pink-400"> *</span> : null}
            </label>
            {renderField(field)}
            {form.formState.errors[field.name] ? (
              <p className="mt-1 text-xs text-rose-400">This field is required.</p>
            ) : null}
          </div>
        ))}
      </div>
      <div className="flex items-center justify-end gap-2">
        {onCancel ? (
          <button
            type="button"
            onClick={onCancel}
            className="rounded border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 hover:bg-slate-800"
          >
            Cancel
          </button>
        ) : null}
        <button
          type="submit"
          className="rounded bg-sky-500 px-4 py-2 text-sm font-semibold text-slate-900 hover:bg-sky-400"
        >
          {submitLabel}
        </button>
      </div>
    </form>
  );
}

export default DynamicForm;


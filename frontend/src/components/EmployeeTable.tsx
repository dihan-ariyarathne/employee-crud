import type { Employee } from '../types/employee.ts';
import type { SchemaField } from '../types/schema.ts';

interface EmployeeTableProps {
  employees: Employee[];
  fields: SchemaField[];
  onEdit: (employee: Employee) => void;
  onDelete: (employee: Employee) => void;
}

function formatValue(value: unknown) {
  if (Array.isArray(value) || (typeof value === 'object' && value !== null)) {
    return JSON.stringify(value, null, 2);
  }
  if (value === null || value === undefined) {
    return '';
  }
  return String(value);
}

function EmployeeTable({ employees, fields, onEdit, onDelete }: EmployeeTableProps) {
  return (
    <div className="overflow-x-auto">
      <table className="min-w-full divide-y divide-slate-800">
        <thead>
          <tr className="bg-slate-900/80">
            <th className="px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400">
              ID
            </th>
            {fields.map((field) => (
              <th
                key={field.name}
                className="px-4 py-2 text-left text-xs font-semibold uppercase tracking-wide text-slate-400"
              >
                {field.name}
              </th>
            ))}
            <th className="px-4 py-2 text-right text-xs font-semibold uppercase tracking-wide text-slate-400">
              Actions
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800">
          {employees.map((employee) => (
            <tr key={employee.id} className="hover:bg-slate-900/60">
              <td className="px-4 py-3 text-sm text-slate-200">{employee.id}</td>
              {fields.map((field) => (
                <td key={field.name} className="px-4 py-3 text-sm text-slate-200">
                  <pre className="whitespace-pre-wrap break-words text-xs leading-relaxed text-slate-300">
                    {formatValue(employee.attributes[field.name])}
                  </pre>
                </td>
              ))}
              <td className="px-4 py-3 text-right text-sm text-slate-200">
                <div className="flex justify-end gap-2">
                  <button
                    type="button"
                    onClick={() => onEdit(employee)}
                    className="rounded border border-slate-600 px-3 py-1 text-xs font-semibold text-slate-100 hover:bg-slate-800"
                  >
                    Edit
                  </button>
                  <button
                    type="button"
                    onClick={() => onDelete(employee)}
                    className="rounded bg-rose-500 px-3 py-1 text-xs font-semibold text-slate-950 hover:bg-rose-400"
                  >
                    Delete
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {employees.length === 0 ? (
        <div className="px-6 py-12 text-center text-sm text-slate-400">No employees found.</div>
      ) : null}
    </div>
  );
}

export default EmployeeTable;


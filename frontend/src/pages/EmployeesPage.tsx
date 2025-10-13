import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import DynamicForm from '../components/DynamicForm.tsx';
import EmployeeTable from '../components/EmployeeTable.tsx';
import FiltersBar from '../components/FiltersBar.tsx';
import PaginationControls from '../components/PaginationControls.tsx';
import ErrorState from '../components/states/ErrorState.tsx';
import LoadingState from '../components/states/LoadingState.tsx';
import { useCreateEmployee, useDeleteEmployee, useEmployees, useReplaceEmployee } from '../hooks/useEmployees.ts';
import { useSchema } from '../hooks/useSchema.ts';
import type { Employee } from '../types/employee.ts';
import type { SchemaField } from '../types/schema.ts';
import { useAuth } from '../context/AuthProvider.tsx';

const PAGE_SIZE = 10;

function EmployeesPage() {
  const navigate = useNavigate();
  const { user, signOut } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [isCreating, setIsCreating] = useState(false);
  const [editing, setEditing] = useState<Employee | null>(null);

  const schemaQuery = useSchema();
  const schemaFields = useMemo<SchemaField[]>(() => {
    if (!schemaQuery.data) return [];
    return Object.values(schemaQuery.data.fields).sort((a, b) => a.name.localeCompare(b.name));
  }, [schemaQuery.data]);

  const employeesQuery = useEmployees({
    page,
    size: PAGE_SIZE,
    sort: 'createdAt',
    direction: 'desc',
    search: search.trim() || undefined,
    filters: filters
  });

  const createEmployee = useCreateEmployee();
  const replaceEmployee = useReplaceEmployee();
  const deleteEmployee = useDeleteEmployee();

  const resetPagination = () => setPage(0);

  const handleCreate = (attributes: Record<string, unknown>) => {
    createEmployee.mutate(
      { attributes },
      {
        onSuccess: () => {
          setIsCreating(false);
        }
      }
    );
  };

  const handleReplace = (attributes: Record<string, unknown>) => {
    if (!editing) return;
    replaceEmployee.mutate(
      { id: editing.id, payload: { attributes } },
      {
        onSuccess: () => {
          setEditing(null);
        }
      }
    );
  };

  const handleDelete = (employee: Employee) => {
    deleteEmployee.mutate(
      { id: employee.id, soft: true },
      {
        onSuccess: () => {
          if (employeesQuery.data && employeesQuery.data.content.length === 1 && page > 0) {
            setPage(page - 1);
          }
        }
      }
    );
  };

  const handleFiltersChange = (nextFilters: Record<string, string>) => {
    setFilters(nextFilters);
    resetPagination();
  };

  const handleSearchChange = (value: string) => {
    setSearch(value);
    resetPagination();
  };

  const handleEdit = (employee: Employee) => {
    setEditing(employee);
    setIsCreating(false);
  };

  if (schemaQuery.isLoading) {
    return (
      <div className="mx-auto max-w-7xl space-y-6 p-6">
        <h1 className="text-2xl font-bold text-slate-100">Employee Directory</h1>
        <LoadingState />
      </div>
    );
  }

  if (schemaQuery.isError || !schemaQuery.data) {
    return (
      <div className="mx-auto max-w-7xl space-y-6 p-6">
        <h1 className="text-2xl font-bold text-slate-100">Employee Directory</h1>
        <ErrorState message="Failed to load schema." onRetry={() => schemaQuery.refetch()} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6 p-6">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-100">Employee Directory</h1>
          <p className="text-sm text-slate-400">Manage Employee Records</p>
        </div>
        <div className="flex items-center gap-2">
          {user ? (
            <span className="hidden text-xs text-slate-400 sm:inline">{user.email}</span>
          ) : null}
          <button
            type="button"
            onClick={() => {
              setIsCreating(true);
              setEditing(null);
            }}
            className="rounded bg-sky-500 px-4 py-2 text-sm font-semibold text-slate-950 hover:bg-sky-400"
          >
            New Employee
          </button>
          <button
            type="button"
            onClick={() => schemaQuery.refetch()}
            className="rounded border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-100 hover:bg-slate-800"
          >
            Refresh Schema
          </button>
          <button
            type="button"
            onClick={async () => {
              await signOut();
              navigate('/login', { replace: true });
            }}
            className="rounded border border-slate-700 px-4 py-2 text-sm font-semibold text-slate-100 hover:bg-slate-800"
          >
            Sign out
          </button>
        </div>
      </header>

      <FiltersBar
        fields={schemaFields}
        search={search}
        activeFilters={filters}
        onSearchChange={handleSearchChange}
        onFiltersChange={handleFiltersChange}
      />

      {isCreating || editing ? (
        <section className="rounded border border-slate-800 bg-slate-950/60 p-6">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold text-slate-100">{editing ? 'Edit Employee' : 'Add Employee'}</h2>
            <div className="space-x-2 text-xs text-slate-400">
              <span>Required fields marked *</span>
            </div>
          </div>
          <DynamicForm
            fields={schemaFields}
            defaultValues={editing?.attributes}
            submitLabel={editing ? 'Update' : 'Create'}
            onCancel={() => {
              setIsCreating(false);
              setEditing(null);
            }}
            onSubmit={(values) => {
              if (editing) {
                handleReplace(values);
              } else {
                handleCreate(values);
              }
            }}
          />
        </section>
      ) : null}

      <section className="rounded border border-slate-800 bg-slate-950/60 p-4">
        {employeesQuery.isLoading ? (
          <LoadingState />
        ) : employeesQuery.isError || !employeesQuery.data ? (
          <ErrorState message="Unable to load employees." onRetry={() => employeesQuery.refetch()} />
        ) : (
          <>
            <EmployeeTable
              employees={employeesQuery.data.content}
              fields={schemaFields}
              onEdit={handleEdit}
              onDelete={handleDelete}
            />
            <PaginationControls
              page={employeesQuery.data.page}
              size={employeesQuery.data.size}
              totalPages={employeesQuery.data.totalPages}
              onPageChange={setPage}
            />
          </>
        )}
      </section>
    </div>
  );
}

export default EmployeesPage;

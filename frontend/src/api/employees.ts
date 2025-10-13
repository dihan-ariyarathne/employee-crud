import http from '../lib/http.ts';
import type { Employee, EmployeeQueryParams, PageResponse } from '../types/employee.ts';

function buildQuery(params: EmployeeQueryParams = {}) {
  const searchParams = new URLSearchParams();
  if (params.page !== undefined) searchParams.set('page', String(params.page));
  if (params.size !== undefined) searchParams.set('size', String(params.size));
  if (params.sort) searchParams.set('sort', params.sort);
  if (params.direction) searchParams.set('direction', params.direction);
  if (params.search) searchParams.set('search', params.search);

  if (params.filters) {
    Object.entries(params.filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        searchParams.set(key, String(value));
      }
    });
  }

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

export async function fetchEmployees(params: EmployeeQueryParams = {}, signal?: AbortSignal) {
  const response = await http.get<PageResponse<Employee>>(`/api/employees${buildQuery(params)}`, {
    signal
  });
  return response.data;
}

export interface UpsertPayload {
  attributes: Record<string, unknown>;
}

export async function createEmployee(payload: UpsertPayload) {
  const response = await http.post<Employee>('/api/employees', payload);
  return response.data;
}

export async function replaceEmployee(id: string, payload: UpsertPayload) {
  const response = await http.put<Employee>(`/api/employees/${id}`, payload);
  return response.data;
}

export async function patchEmployee(id: string, payload: UpsertPayload) {
  const response = await http.patch<Employee>(`/api/employees/${id}`, payload);
  return response.data;
}

export async function deleteEmployee(id: string, soft = true) {
  await http.delete(`/api/employees/${id}`, { params: { soft } });
}


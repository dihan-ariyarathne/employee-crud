import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';

import {
  createEmployee,
  deleteEmployee,
  fetchEmployees,
  patchEmployee,
  replaceEmployee,
  type EmployeeQueryParams,
  type UpsertPayload
} from '../api/employees.ts';

export function useEmployees(params: EmployeeQueryParams) {
  return useQuery({
    queryKey: ['employees', params],
    queryFn: ({ signal }) => fetchEmployees(params, signal),
    placeholderData: keepPreviousData
  });
}

export function useCreateEmployee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpsertPayload) => createEmployee(payload),
    onSuccess: () => {
      toast.success('Employee created');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
    },
    onError: (error: unknown) => {
      toast.error(error instanceof Error ? error.message : 'Failed to create employee');
    }
  });
}

export function useReplaceEmployee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpsertPayload }) => replaceEmployee(id, payload),
    onSuccess: () => {
      toast.success('Employee updated');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
    },
    onError: (error: unknown) => {
      toast.error(error instanceof Error ? error.message : 'Failed to update employee');
    }
  });
}

export function usePatchEmployee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpsertPayload }) => patchEmployee(id, payload),
    onSuccess: () => {
      toast.success('Employee patched');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
    },
    onError: (error: unknown) => {
      toast.error(error instanceof Error ? error.message : 'Failed to patch employee');
    }
  });
}

export function useDeleteEmployee() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, soft }: { id: string; soft?: boolean }) => deleteEmployee(id, soft),
    onSuccess: () => {
      toast.success('Employee deleted');
      queryClient.invalidateQueries({ queryKey: ['employees'] });
    },
    onError: (error: unknown) => {
      toast.error(error instanceof Error ? error.message : 'Failed to delete employee');
    }
  });
}


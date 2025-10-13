export interface EmployeeAttributes {
  [key: string]: unknown;
}

export interface Employee {
  id: string;
  attributes: EmployeeAttributes;
  deleted: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface EmployeeQueryParams {
  page?: number;
  size?: number;
  sort?: string;
  direction?: 'asc' | 'desc';
  search?: string;
  filters?: Record<string, string>;
}


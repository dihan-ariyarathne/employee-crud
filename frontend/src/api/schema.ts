import http from '../lib/http.ts';
import type { SchemaResult } from '../types/schema.ts';

export async function fetchSchema(signal?: AbortSignal) {
  const response = await http.get<SchemaResult>('/api/schema', { signal });
  return response.data;
}


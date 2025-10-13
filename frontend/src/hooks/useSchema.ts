import { useQuery } from '@tanstack/react-query';

import { fetchSchema } from '../api/schema.ts';

export function useSchema() {
  return useQuery({
    queryKey: ['schema'],
    queryFn: ({ signal }) => fetchSchema(signal)
  });
}


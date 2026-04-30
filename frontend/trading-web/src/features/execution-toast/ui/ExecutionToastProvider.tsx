import { useCallback } from 'react';
import type { ExecutionEvent } from '../../../entities/execution/model/types';
import { useExecutionSse } from '../../../shared/lib/sse/useExecutionSse';
import { useToastStore } from '../model/useToastStore';

export function ExecutionToastProvider() {
  const baseUrl = import.meta.env.VITE_API_BASE_URL ?? '';

  const handleEvent = useCallback((event: ExecutionEvent) => {
    useToastStore.getState().addToast(event);
  }, []);

  useExecutionSse(`${baseUrl}/api/v1/executions/stream`, handleEvent);

  return null;
}

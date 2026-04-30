import type { ExecutionEvent } from '../../../entities/execution/model/types';

export interface Toast {
  id: string;
  event: ExecutionEvent;
}

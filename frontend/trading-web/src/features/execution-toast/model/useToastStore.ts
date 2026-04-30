import { create } from 'zustand';
import type { ExecutionEvent } from '../../../entities/execution/model/types';
import type { Toast } from '../../../shared/ui/Toast/types';

export type { Toast };

const MAX_TOASTS = 5;

interface ToastState {
  toasts: Toast[];
  addToast: (event: ExecutionEvent) => void;
  removeToast: (id: string) => void;
}

export const useToastStore = create<ToastState>((set) => ({
  toasts: [],
  addToast: (event) =>
    set((state) => {
      const newToast: Toast = { id: `${Date.now()}-${Math.random()}`, event };
      const updated = [newToast, ...state.toasts];
      return { toasts: updated.slice(0, MAX_TOASTS) };
    }),
  removeToast: (id) =>
    set((state) => ({
      toasts: state.toasts.filter((t) => t.id !== id),
    })),
}));

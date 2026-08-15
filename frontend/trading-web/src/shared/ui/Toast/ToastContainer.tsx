import type { Toast } from './types';
import { ToastItem } from './ToastItem';

interface ToastContainerProps {
  toasts: Toast[];
  onDismiss: (id: string) => void;
}

export function ToastContainer({ toasts, onDismiss }: ToastContainerProps) {
  return (
    <div
      className="toast-container pointer-events-none fixed top-4 right-4 z-9999 flex flex-col gap-2"
      aria-label="체결 알림"
      aria-live="polite"
    >
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onDismiss={onDismiss} />
      ))}
    </div>
  );
}

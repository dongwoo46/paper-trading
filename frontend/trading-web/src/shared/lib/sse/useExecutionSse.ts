import { useEffect, useState } from 'react';
import type { ExecutionEvent } from '../../../entities/execution/model/types';

export function useExecutionSse(
  url: string,
  onEvent: (event: ExecutionEvent) => void,
): { connected: boolean } {
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const source = new EventSource(url);

    source.onopen = () => {
      setConnected(true);
    };

    source.addEventListener('execution', (e: MessageEvent) => {
      try {
        const data = JSON.parse(e.data) as ExecutionEvent;
        onEvent(data);
      } catch (err) {
        console.error('[useExecutionSse] Failed to parse execution event:', err);
      }
    });

    source.onerror = () => {
      setConnected(false);
      // EventSource auto-reconnects; no manual retry needed
    };

    return () => {
      source.close();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [url]);

  return { connected };
}

import { create } from "zustand";
import type { LlmDoneEvent } from "../api/chartAnalysisApi";

export type AppNotification = {
  id: string;
  symbol: string;
  name: string;
  window: string;
  interval: string;
  source: string;
  read: boolean;
  createdAt: Date;
};

interface NotificationState {
  notifications: AppNotification[];
  unreadCount: number;
  addFromLlmDone: (event: LlmDoneEvent) => void;
  markAllRead: () => void;
  clear: () => void;
}

const MAX_NOTIFICATIONS = 20;

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],
  unreadCount: 0,
  addFromLlmDone: (event) =>
    set((state) => {
      const notification: AppNotification = {
        id: `${Date.now()}-${Math.random()}`,
        symbol: event.symbol,
        name: event.name,
        window: event.window,
        interval: event.interval,
        source: event.source,
        read: false,
        createdAt: new Date(),
      };
      const updated = [notification, ...state.notifications].slice(0, MAX_NOTIFICATIONS);
      return { notifications: updated, unreadCount: state.unreadCount + 1 };
    }),
  markAllRead: () =>
    set((state) => ({
      notifications: state.notifications.map((n) => ({ ...n, read: true })),
      unreadCount: 0,
    })),
  clear: () => set({ notifications: [], unreadCount: 0 }),
}));

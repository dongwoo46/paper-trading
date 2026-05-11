import { create } from "zustand";

export type IndicatorKey = "bb" | "rsi" | "macd" | "volume" | "investorFlow" | "ma5" | "ma20" | "ma60" | "ma120";

interface IndicatorState {
  active: Record<IndicatorKey, boolean>;
  toggle: (key: IndicatorKey) => void;
  setAll: (patch: Partial<Record<IndicatorKey, boolean>>) => void;
}

const initialActive: Record<IndicatorKey, boolean> = {
  volume: true,
  bb: false,
  rsi: false,
  macd: false,
  investorFlow: false,
  ma5: true,
  ma20: true,
  ma60: true,
  ma120: true,
};

export const useIndicatorStore = create<IndicatorState>()((set) => ({
  active: initialActive,
  toggle: (key) =>
    set((state) => ({
      active: { ...state.active, [key]: !state.active[key] },
    })),
  setAll: (patch) =>
    set((state) => ({
      active: { ...state.active, ...patch },
    })),
}));

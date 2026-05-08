import { create } from "zustand";
import { persist } from "zustand/middleware";

type DrawPoint = { logical: number; price: number };
export type TrendLine = { id: string; p1: DrawPoint; p2: DrawPoint };
export type HorizontalLine = { id: string; price: number };

type ChartKey = string; // `${source}-${symbol}-${interval}`

interface ChartDrawState {
  trendLines: Record<ChartKey, TrendLine[]>;
  hLines: Record<ChartKey, HorizontalLine[]>;
  getTrendLines: (key: ChartKey) => TrendLine[];
  getHLines: (key: ChartKey) => HorizontalLine[];
  addTrendLine: (key: ChartKey, line: TrendLine) => void;
  addHLine: (key: ChartKey, line: HorizontalLine) => void;
  setTrendLines: (key: ChartKey, lines: TrendLine[]) => void;
  setHLines: (key: ChartKey, lines: HorizontalLine[]) => void;
  clearLines: (key: ChartKey) => void;
}

export const useChartDrawStore = create<ChartDrawState>()(
  persist(
    (set, get) => ({
      trendLines: {},
      hLines: {},
      getTrendLines: (key) => get().trendLines[key] ?? [],
      getHLines: (key) => get().hLines[key] ?? [],
      addTrendLine: (key, line) =>
        set((state) => ({
          trendLines: {
            ...state.trendLines,
            [key]: [...(state.trendLines[key] ?? []), line],
          },
        })),
      addHLine: (key, line) =>
        set((state) => ({
          hLines: {
            ...state.hLines,
            [key]: [...(state.hLines[key] ?? []), line],
          },
        })),
      setTrendLines: (key, lines) =>
        set((state) => ({
          trendLines: { ...state.trendLines, [key]: lines },
        })),
      setHLines: (key, lines) =>
        set((state) => ({
          hLines: { ...state.hLines, [key]: lines },
        })),
      clearLines: (key) =>
        set((state) => ({
          trendLines: { ...state.trendLines, [key]: [] },
          hLines: { ...state.hLines, [key]: [] },
        })),
    }),
    { name: "chart-drawing-store" }
  )
);

import { create } from "zustand";
import { persist } from "zustand/middleware";

type DrawPoint = { logical: number; price: number };
export type TrendLine = { id: string; p1: DrawPoint; p2: DrawPoint };
export type HorizontalLine = { id: string; price: number };
export type CrossLine = { id: string; logical: number; price: number };

type ChartKey = string; // `${source}-${symbol}-${interval}`

interface ChartDrawState {
  trendLines: Record<ChartKey, TrendLine[]>;
  hLines: Record<ChartKey, HorizontalLine[]>;
  crossLines: Record<ChartKey, CrossLine[]>;
  getTrendLines: (key: ChartKey) => TrendLine[];
  getHLines: (key: ChartKey) => HorizontalLine[];
  getCrossLines: (key: ChartKey) => CrossLine[];
  addTrendLine: (key: ChartKey, line: TrendLine) => void;
  addHLine: (key: ChartKey, line: HorizontalLine) => void;
  addCrossLine: (key: ChartKey, line: CrossLine) => void;
  setTrendLines: (key: ChartKey, lines: TrendLine[]) => void;
  setHLines: (key: ChartKey, lines: HorizontalLine[]) => void;
  setCrossLines: (key: ChartKey, lines: CrossLine[]) => void;
  clearLines: (key: ChartKey) => void;
}

export const useChartDrawStore = create<ChartDrawState>()(
  persist(
    (set, get) => ({
      trendLines: {},
      hLines: {},
      crossLines: {},
      getTrendLines: (key) => get().trendLines[key] ?? [],
      getHLines: (key) => get().hLines[key] ?? [],
      getCrossLines: (key) => get().crossLines[key] ?? [],
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
      addCrossLine: (key, line) =>
        set((state) => ({
          crossLines: {
            ...state.crossLines,
            [key]: [...(state.crossLines[key] ?? []), line],
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
      setCrossLines: (key, lines) =>
        set((state) => ({
          crossLines: { ...state.crossLines, [key]: lines },
        })),
      clearLines: (key) =>
        set((state) => ({
          trendLines: { ...state.trendLines, [key]: [] },
          hLines: { ...state.hLines, [key]: [] },
          crossLines: { ...state.crossLines, [key]: [] },
        })),
    }),
    { name: "chart-drawing-store" }
  )
);

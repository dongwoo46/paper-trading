import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  CandlestickSeries,
  ColorType,
  CrosshairMode,
  createChart,
  type IChartApi,
  type ISeriesApi,
  type Logical,
  type MouseEventParams,
  type Time,
} from "lightweight-charts";
import {
  fetchDailyBars,
  fetchMarketSymbols,
  fetchMinuteBars,
  fetchWeeklyBars,
  type MarketSource,
  type OhlcvPoint,
  type UnifiedInterval,
} from "../../../shared/api/marketUnifiedApi";
import { useChartDrawStore } from "../../../features/chart-drawing/model/useChartDrawStore";
import type {
  TrendLine,
  HorizontalLine,
} from "../../../features/chart-drawing/model/useChartDrawStore";

const INTERVAL_LABEL: Record<UnifiedInterval, string> = {
  "1m": "1분봉",
  "5m": "5분봉",
  "10m": "10분봉",
  "1d": "일봉",
  "1w": "주봉",
};

function formatNumber(value: number): string {
  return new Intl.NumberFormat("ko-KR", { maximumFractionDigits: 2 }).format(
    value,
  );
}

type CrosshairInfo = {
  startedAt: string;
  open: number;
  high: number;
  low: number;
  close: number;
  volume: number;
};

type DrawTool = "none" | "trend" | "hline";

type DrawPoint = {
  logical: number;
  price: number;
};

function toChartTime(startedAt: string): Time {
  const normalized = startedAt.replace(" ", "T");
  const d = new Date(normalized);
  return Math.floor(d.getTime() / 1000) as Time;
}

function UnifiedCandlestickChart({
  bars,
  drawTool,
  chartKey,
  onCrosshairChange,
}: {
  bars: OhlcvPoint[];
  drawTool: DrawTool;
  chartKey: string;
  onCrosshairChange: (value: CrosshairInfo | null) => void;
}) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const seriesRef = useRef<ISeriesApi<"Candlestick"> | null>(null);

  // Zustand persistent state
  const trendLines = useChartDrawStore((s) => s.trendLines[chartKey] ?? []);
  const hLines = useChartDrawStore((s) => s.hLines[chartKey] ?? []);
  const addTrendLine = useChartDrawStore((s) => s.addTrendLine);
  const addHLine = useChartDrawStore((s) => s.addHLine);
  const clearLines = useChartDrawStore((s) => s.clearLines);

  // pendingTrendStart is ephemeral - reset on navigation
  const [pendingTrendStart, setPendingTrendStart] = useState<DrawPoint | null>(
    null,
  );

  // Force re-render for overlay recalculation on range change
  const [, forceUpdate] = useState(0);

  const overlay = useMemo(() => {
    const chart = chartRef.current;
    const series = seriesRef.current;
    if (!chart || !series)
      return {
        trendSegments: [],
        hSegments: [] as Array<{ id: string; y: number }>,
      };

    const trendSegments = trendLines
      .map((line: TrendLine) => {
        const x1 = chart
          .timeScale()
          .logicalToCoordinate(line.p1.logical as unknown as Logical);
        const x2 = chart
          .timeScale()
          .logicalToCoordinate(line.p2.logical as unknown as Logical);
        const y1 = series.priceToCoordinate(line.p1.price);
        const y2 = series.priceToCoordinate(line.p2.price);
        if (x1 == null || x2 == null || y1 == null || y2 == null) return null;
        return {
          id: line.id,
          x1: Number(x1),
          y1: Number(y1),
          x2: Number(x2),
          y2: Number(y2),
        };
      })
      .filter((v) => v !== null);

    const hSegments = hLines
      .map((line: HorizontalLine) => {
        const y = series.priceToCoordinate(line.price);
        if (y == null) return null;
        return { id: line.id, y: Number(y) };
      })
      .filter((v) => v !== null);

    return { trendSegments, hSegments };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trendLines, hLines, bars]);

  useEffect(() => {
    if (!rootRef.current) return;
    const root = rootRef.current;
    const chart = createChart(root, {
      width: root.clientWidth,
      height: 360,
      layout: {
        background: { type: ColorType.Solid, color: "#0b1220" },
        textColor: "#d1d5db",
      },
      grid: {
        vertLines: { color: "rgba(255,255,255,0.06)" },
        horzLines: { color: "rgba(255,255,255,0.06)" },
      },
      crosshair: { mode: CrosshairMode.Normal },
      rightPriceScale: {
        borderColor: "rgba(255,255,255,0.2)",
      },
      timeScale: {
        borderColor: "rgba(255,255,255,0.2)",
        timeVisible: true,
        secondsVisible: false,
      },
      handleScroll: {
        mouseWheel: true,
        pressedMouseMove: true,
        horzTouchDrag: true,
        vertTouchDrag: true,
      },
      handleScale: {
        mouseWheel: true,
        pinch: true,
        axisPressedMouseMove: true,
      },
    });

    const series = chart.addSeries(CandlestickSeries, {
      upColor: "#22c55e",
      downColor: "#ef4444",
      borderVisible: true,
      wickUpColor: "#22c55e",
      wickDownColor: "#ef4444",
    });

    chartRef.current = chart;
    seriesRef.current = series;

    const onResize = () => {
      chart.applyOptions({ width: root.clientWidth });
    };
    const ro = new ResizeObserver(onResize);
    ro.observe(root);

    return () => {
      ro.disconnect();
      chart.remove();
      chartRef.current = null;
      seriesRef.current = null;
    };
  }, []);

  useEffect(() => {
    const chart = chartRef.current;
    const series = seriesRef.current;
    if (!chart || !series) return;

    const data = bars.map((bar) => ({
      time: toChartTime(bar.startedAt),
      open: bar.open,
      high: bar.high,
      low: bar.low,
      close: bar.close,
    }));
    series.setData(data);
    chart.timeScale().fitContent();
  }, [bars]);

  useEffect(() => {
    const chart = chartRef.current;
    const series = seriesRef.current;
    if (!chart || !series) return;

    const onMove = (param: MouseEventParams<Time>) => {
      const point = param.seriesData.get(series);
      if (!point || !("open" in point)) {
        onCrosshairChange(null);
        return;
      }

      const time = typeof param.time === "number" ? param.time : null;
      const startedAt =
        time != null
          ? new Date(time * 1000).toISOString().replace("T", " ").slice(0, 19)
          : "-";
      const index = bars.findIndex(
        (b) => toChartTime(b.startedAt) === param.time,
      );
      const volume = index >= 0 ? bars[index].volume : 0;

      onCrosshairChange({
        startedAt,
        open: point.open,
        high: point.high,
        low: point.low,
        close: point.close,
        volume,
      });
    };

    const onClick = (param: MouseEventParams<Time>) => {
      if (drawTool === "none" || !param.point) return;
      const logical = chart.timeScale().coordinateToLogical(param.point.x);
      const price = series.coordinateToPrice(param.point.y);
      if (logical == null || price == null) return;

      if (drawTool === "hline") {
        addHLine(chartKey, { id: crypto.randomUUID(), price });
        return;
      }

      if (!pendingTrendStart) {
        setPendingTrendStart({ logical: Number(logical), price });
        return;
      }

      addTrendLine(chartKey, {
        id: crypto.randomUUID(),
        p1: pendingTrendStart,
        p2: { logical: Number(logical), price },
      });
      setPendingTrendStart(null);
    };

    const onRangeChanged = () => {
      forceUpdate((n) => n + 1);
    };

    chart.subscribeCrosshairMove(onMove);
    chart.subscribeClick(onClick);
    chart.timeScale().subscribeVisibleLogicalRangeChange(onRangeChanged);

    return () => {
      chart.unsubscribeCrosshairMove(onMove);
      chart.unsubscribeClick(onClick);
      chart.timeScale().unsubscribeVisibleLogicalRangeChange(onRangeChanged);
    };
  }, [
    bars,
    drawTool,
    pendingTrendStart,
    onCrosshairChange,
    chartKey,
    addTrendLine,
    addHLine,
  ]);

  useEffect(() => {
    if (drawTool !== "trend") {
      setPendingTrendStart(null);
    }
  }, [drawTool]);

  return (
    <div className="relative w-full h-[360px]">
      <div className="w-full h-[360px]" ref={rootRef} />
      <svg
        className="absolute inset-0 w-full h-[360px] pointer-events-none"
        viewBox="0 0 100 100"
        preserveAspectRatio="none"
        aria-hidden="true"
      >
        {overlay.hSegments.map((line) => (
          <line
            key={line.id}
            x1="0"
            y1={(line.y / 360) * 100}
            x2="100"
            y2={(line.y / 360) * 100}
            className="[vector-effect:non-scaling-stroke] stroke-cyan-400 [stroke-dasharray:6_4] [stroke-width:1.4]"
          />
        ))}
        {overlay.trendSegments.map((line) => (
          <line
            key={line.id}
            x1={
              (line.x1 / Math.max(rootRef.current?.clientWidth ?? 1, 1)) * 100
            }
            y1={(line.y1 / 360) * 100}
            x2={
              (line.x2 / Math.max(rootRef.current?.clientWidth ?? 1, 1)) * 100
            }
            y2={(line.y2 / 360) * 100}
            className="[vector-effect:non-scaling-stroke] stroke-amber-500 [stroke-width:1.8]"
          />
        ))}
      </svg>
      {(trendLines.length > 0 || hLines.length > 0) && (
        <button
          type="button"
          className="absolute right-2 top-2 inline-flex items-center justify-center gap-2.5 px-4 py-2 rounded-xl font-semibold text-sm cursor-pointer transition-all border border-white/12 text-text-primary bg-transparent hover:bg-white/5 hover:border-text-muted"
          onClick={() => {
            clearLines(chartKey);
            setPendingTrendStart(null);
          }}
        >
          선 전체 삭제
        </button>
      )}
    </div>
  );
}

export function MarketUnifiedChartPage() {
  const [source, setSource] = useState<MarketSource>("yfinance");
  const [keyword, setKeyword] = useState("");
  const [selectedSymbol, setSelectedSymbol] = useState("AAPL");
  const [interval, setInterval] = useState<UnifiedInterval>("1d");
  const [minuteLimit, setMinuteLimit] = useState(100);
  const [drawTool, setDrawTool] = useState<DrawTool>("none");
  const [crosshairInfo, setCrosshairInfo] = useState<CrosshairInfo | null>(
    null,
  );

  const chartKey = `${source}-${selectedSymbol}-${interval}`;

  const symbolsQuery = useQuery({
    queryKey: ["market-symbols", source],
    queryFn: () => fetchMarketSymbols(source, 300),
  });

  const filteredSymbols = useMemo(() => {
    const list = symbolsQuery.data ?? [];
    const q = keyword.trim().toLowerCase();
    if (!q) return list.slice(0, 80);
    return list
      .filter(
        (item) =>
          item.symbol.toLowerCase().includes(q) ||
          item.name.toLowerCase().includes(q),
      )
      .slice(0, 80);
  }, [symbolsQuery.data, keyword]);

  const barsQuery = useQuery({
    queryKey: [
      "market-unified-bars",
      source,
      selectedSymbol,
      interval,
      minuteLimit,
    ],
    enabled: selectedSymbol.trim().length > 0,
    queryFn: async () => {
      if (interval === "1d") {
        return fetchDailyBars(source, selectedSymbol);
      }
      if (interval === "1w") {
        return fetchWeeklyBars(source, selectedSymbol, 130);
      }
      return fetchMinuteBars(selectedSymbol, interval, minuteLimit);
    },
  });

  const bars = barsQuery.data ?? [];
  const last = bars[bars.length - 1];

  const ACTIVE_TOOL_CLS = "border-blue-400 text-blue-100 bg-blue-500/[0.16]";
  const BTN_BASE =
    "inline-flex items-center justify-center gap-2.5 px-4 py-2 rounded-xl font-semibold text-sm cursor-pointer transition-all border whitespace-nowrap";
  const BTN_OUTLINE =
    "bg-transparent border-white/12 text-text-primary hover:bg-white/5 hover:border-text-muted";

  return (
    <section className="flex flex-col gap-5 animate-fade-in">
      <div className="flex flex-col gap-1.5">
        <h2 className="text-[28px] font-bold tracking-tight">통합 시세 차트</h2>
        <p className="text-text-secondary text-[15px] max-w-3xl">
          종목 선택 후 주봉/일봉/1·5·10분봉을 한 화면에서 전환해서 확인합니다.
        </p>
      </div>

      <div className="grid grid-cols-[360px_1fr] gap-4 max-lg:grid-cols-1">
        {/* Left column */}
        <div className="flex flex-col gap-3">
          <div className="bg-bg-card border border-white/12 rounded-[20px] p-4">
            <div className="grid grid-cols-[1fr_auto_auto] gap-2 max-lg:grid-cols-1">
              <input
                className="bg-bg-input border border-white/12 text-text-primary px-4 py-3 rounded-xl outline-none transition-all w-full focus:border-brand-primary focus:shadow-[0_0_0_4px_rgba(96,165,250,0.25)] focus:bg-bg-card"
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                placeholder="종목 검색 (예: AAPL, 005930)"
              />
              <select
                className="bg-bg-input border border-white/12 text-text-primary px-4 py-3 rounded-xl outline-none transition-all focus:border-brand-primary focus:shadow-[0_0_0_4px_rgba(96,165,250,0.25)] focus:bg-bg-card"
                value={source}
                onChange={(e) => setSource(e.target.value as MarketSource)}
              >
                <option value="yfinance">yfinance</option>
                <option value="pykrx">pykrx</option>
              </select>
              <button
                className={`${BTN_BASE} ${BTN_OUTLINE}`}
                onClick={() => symbolsQuery.refetch()}
              >
                새로고침
              </button>
            </div>
          </div>

          <section className="bg-bg-card border border-white/12 rounded-[20px] flex flex-col overflow-hidden shadow-md">
            <div className="px-6 py-5 border-b border-white/12 flex items-center justify-between bg-white/[0.01]">
              <h3 className="text-[17px] font-semibold text-text-primary">
                종목 테이블
              </h3>
            </div>
            <div className="max-h-[360px] overflow-auto">
              <table className="w-full border-collapse">
                <thead>
                  <tr>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">
                      심볼
                    </th>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">
                      이름
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {filteredSymbols.map((item) => (
                    <tr
                      key={`${item.symbol}-${item.name}`}
                      className={`cursor-pointer ${selectedSymbol === item.symbol ? "bg-blue-500/[0.14]" : "hover:bg-white/[0.02]"}`}
                      onClick={() => setSelectedSymbol(item.symbol)}
                    >
                      <td
                        className={`px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap ${selectedSymbol === item.symbol ? "text-blue-100" : "text-text-secondary"}`}
                      >
                        {item.symbol}
                      </td>
                      <td
                        className={`px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap ${selectedSymbol === item.symbol ? "text-blue-100" : "text-text-secondary"}`}
                      >
                        {item.name}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>

        {/* Right column */}
        <div className="flex flex-col gap-3">
          <div className="bg-bg-card border border-white/12 rounded-[20px] p-4">
            {/* Interval tabs */}
            <div className="flex flex-wrap gap-2">
              {(["1w", "1d", "1m", "5m", "10m"] as UnifiedInterval[]).map(
                (v) => (
                  <button
                    key={v}
                    className={`${BTN_BASE} ${interval === v ? "bg-bg-card text-brand-primary shadow-md border border-white/12" : BTN_OUTLINE}`}
                    onClick={() => setInterval(v)}
                    type="button"
                  >
                    {INTERVAL_LABEL[v]}
                  </button>
                ),
              )}
              {(interval === "1m" ||
                interval === "5m" ||
                interval === "10m") && (
                <select
                  aria-label="분봉 조회 개수"
                  className="bg-bg-input border border-white/12 text-text-primary px-3 py-2 rounded-xl outline-none transition-all focus:border-brand-primary focus:bg-bg-card"
                  value={minuteLimit}
                  onChange={(e) => setMinuteLimit(Number(e.target.value))}
                >
                  <option value={50}>50개</option>
                  <option value={100}>100개</option>
                </select>
              )}
            </div>

            {/* Draw tools */}
            <div className="flex flex-wrap gap-2 mt-2">
              <button
                type="button"
                className={`${BTN_BASE} ${drawTool === "none" ? ACTIVE_TOOL_CLS : BTN_OUTLINE}`}
                onClick={() => setDrawTool("none")}
              >
                선택
              </button>
              <button
                type="button"
                className={`${BTN_BASE} ${drawTool === "trend" ? ACTIVE_TOOL_CLS : BTN_OUTLINE}`}
                onClick={() => setDrawTool("trend")}
              >
                추세선
              </button>
              <button
                type="button"
                className={`${BTN_BASE} ${drawTool === "hline" ? ACTIVE_TOOL_CLS : BTN_OUTLINE}`}
                onClick={() => setDrawTool("hline")}
              >
                수평선
              </button>
            </div>

            {/* Meta info */}
            <div className="flex gap-2 flex-wrap mt-2">
              <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                선택 종목: {selectedSymbol || "-"}
              </span>
              <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                주기: {INTERVAL_LABEL[interval]}
              </span>
              <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                데이터: {bars.length}건
              </span>
              {last && (
                <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                  최신 종가: {formatNumber(last.close)}
                </span>
              )}
              {crosshairInfo && (
                <>
                  <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                    포인트: {crosshairInfo.startedAt}
                  </span>
                  <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                    O: {formatNumber(crosshairInfo.open)}
                  </span>
                  <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                    H: {formatNumber(crosshairInfo.high)}
                  </span>
                  <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                    L: {formatNumber(crosshairInfo.low)}
                  </span>
                  <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                    C: {formatNumber(crosshairInfo.close)}
                  </span>
                  <span className="text-xs text-text-secondary border border-white/12 rounded-full px-2.5 py-1">
                    V: {formatNumber(crosshairInfo.volume)}
                  </span>
                </>
              )}
            </div>
          </div>

          {/* Chart area */}
          <div className="w-full min-h-[380px] border border-white/12 rounded-[16px] bg-white/[0.02] p-3">
            {barsQuery.isLoading && <p>차트 데이터를 불러오는 중...</p>}
            {barsQuery.isError && <p>차트 조회 중 오류가 발생했습니다.</p>}
            {!barsQuery.isLoading &&
              !barsQuery.isError &&
              bars.length === 0 && <p>데이터가 없습니다.</p>}
            {!barsQuery.isLoading && !barsQuery.isError && bars.length > 0 && (
              <UnifiedCandlestickChart
                bars={bars}
                drawTool={drawTool}
                chartKey={chartKey}
                onCrosshairChange={setCrosshairInfo}
              />
            )}
          </div>

          {/* OHLCV table */}
          <section className="bg-bg-card border border-white/12 rounded-[20px] flex flex-col overflow-hidden shadow-md">
            <div className="px-6 py-5 border-b border-white/12 flex items-center justify-between bg-white/[0.01]">
              <h3 className="text-[17px] font-semibold text-text-primary">
                OHLCV 테이블
              </h3>
            </div>
            <div className="overflow-x-auto rounded-[16px] bg-black/20 border border-white/12 flex-1 min-h-[300px]">
              <table className="w-full border-collapse">
                <thead>
                  <tr>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">
                      시각/일자
                    </th>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">
                      시가
                    </th>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">
                      고가
                    </th>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">
                      저가
                    </th>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">
                      종가
                    </th>
                    <th className="bg-white/[0.02] px-6 py-3.5 text-left text-[11px] uppercase tracking-widest text-text-muted font-bold border-b border-white/12 whitespace-nowrap">
                      거래량
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {bars
                    .slice()
                    .reverse()
                    .map((bar) => (
                      <tr
                        key={`${bar.startedAt}-${bar.close}`}
                        className="hover:bg-white/[0.02]"
                      >
                        <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary">
                          {bar.startedAt}
                        </td>
                        <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary">
                          {formatNumber(bar.open)}
                        </td>
                        <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary">
                          {formatNumber(bar.high)}
                        </td>
                        <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary">
                          {formatNumber(bar.low)}
                        </td>
                        <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary">
                          {formatNumber(bar.close)}
                        </td>
                        <td className="px-6 py-4 text-[14.5px] border-b border-white/12 whitespace-nowrap text-text-secondary">
                          {formatNumber(bar.volume)}
                        </td>
                      </tr>
                    ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      </div>
    </section>
  );
}
